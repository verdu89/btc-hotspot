/*
 * btc-hotspot
 * Copyright (C) 2016  Andrea Raspitzu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package wallet

import java.io.File
import java.time.{ LocalDate, LocalDateTime }
import java.util.Date

import com.google.protobuf.ByteString
import com.typesafe.scalalogging.LazyLogging
import commons.Configuration.WalletConfig._
import commons.Configuration.MiniPortalConfig._
import org.bitcoin.protocols.payments.Protos
import org.bitcoin.protocols.payments.Protos.PaymentRequest
import org.bitcoinj.core._
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.protocols.payments.PaymentProtocol
import org.bitcoinj.wallet.KeyChain.KeyPurpose
import protocol.domain.{ BitcoinTransaction, Offer, QtyUnit, Session }
import services.{ OfferServiceRegistry, SessionServiceRegistry }

import scala.collection.JavaConverters._
import commons.AppExecutionContextRegistry.context._
import commons.Helpers
import commons.Helpers.FutureOption

import scala.concurrent.{ Future, Promise }
import org.bitcoinj.core.listeners.DownloadProgressTracker
import registry.Registry

object WalletServiceRegistry extends Registry with WalletServiceComponent {

  override val walletService: WalletServiceInterface = new WalletServiceImpl

}

trait WalletServiceComponent {

  val walletService: WalletServiceInterface

}

trait WalletServiceInterface {

  def generatePaymentRequest(session: Session, offerId: Long): Future[PaymentRequest]

  def validateBIP70Payment(payment: Protos.Payment): FutureOption[Protos.PaymentACK]

  def getBalance(): Long //satoshis

  def getTransactions(): Seq[BitcoinTransaction]

  def createSpendingTx(address: String, value: Long): Future[String]

}

class WalletServiceImpl extends WalletServiceInterface with LazyLogging {

  private val file = new File(walletDir)

  private val kit = new WalletAppKit(network, file, walletFileName) {
    override def onSetupCompleted() {
      wallet.importKey(new ECKey)
    }
  }.setAutoStop(true)
    .setDownloadListener(new DownloadProgressTracker {
      override def progress(pct: Double, blocksSoFar: Int, date: Date) = {
        logger.warn(s"Chain sync ${pct * 100}%")
      }
    })

  Helpers.addShutDownHook {
    logger.info("Shutting down bitcoinj peer group")
    kit.peerGroup.stop
  }

  if (isEnabled)
    kit.startAsync

  private def networkParams: NetworkParameters = kit.params
  private def peerGroup: PeerGroup = kit.peerGroup
  private def wallet: org.bitcoinj.wallet.Wallet = kit.wallet
  private def receivingAddress: Address = wallet.currentAddress(KeyPurpose.RECEIVE_FUNDS)

  def generatePaymentRequest(session: Session, offerId: Long): Future[PaymentRequest] = {
    logger.info(s"Issuing payment request for session ${session.id} and offer $offerId")

    (for {
      offer <- OfferServiceRegistry.offerService.offerById(offerId)
    } yield PaymentProtocol.createPaymentRequest(
      networkParams,
      outputsForOffer(offer).asJava,
      s"Please pay ${offer.price} satoshis for ${offer.description}",
      s"http://$miniPortalHost:$miniPortalPort/api/pay/$offerId",
      Array.emptyByteArray
    ).build).future.map(_.getOrElse(throw new IllegalArgumentException(s"Offer $offerId not found")))

  }

  def validateBIP70Payment(payment: Protos.Payment): FutureOption[Protos.PaymentACK] = {
    if (payment.getTransactionsCount != 1)
      throw new IllegalStateException("Too many tx received in payment session")

    val txBytes = payment.getTransactions(0).toByteArray
    val tx = new Transaction(networkParams, txBytes)
    val broadcast = peerGroup.broadcastTransaction(tx)

    val promise = Promise[Protos.PaymentACK]

    broadcast.setProgressCallback((progress: Double) => {
      logger.info(s"tx ${tx.getHashAsString} broadcast for at ${progress * 100}%")
      if (progress == 1.0) {
        promise.completeWith(Future.successful(
          PaymentProtocol.createPaymentAck(payment, s"Enjoy your session!"))
        )
      }
    })

    promise.future.map(Some(_))
  }

  override def getBalance(): Long = {
    wallet.getBalance.value
  }

  def getTransactions(): Seq[BitcoinTransaction] = {
    wallet.getWalletTransactions.asScala.toSeq.map { tx =>
      BitcoinTransaction(
        hash = tx.getTransaction.getHashAsString,
        value = tx.getTransaction.getValue(wallet).value,
        creationDate = {
          if (tx.getTransaction.getConfidence.getDepthInBlocks > 0)
            Some(tx.getTransaction.getUpdateTime)
          else
            None
        }
      )
    }.sortWith((lh, rh) => (lh.creationDate, rh.creationDate) match {
      case (Some(lhDate), Some(rhDate)) => lhDate.after(rhDate)
      case (None, _)                    => true
      case (_, None)                    => false
    })

  }

  override def createSpendingTx(address: String, value: Long): Future[String] = {
    val spendingTx = wallet.createSend(
      Address.fromBase58(networkParams, address),
      Coin.valueOf(value)
    )

    val txHash = spendingTx.getHashAsString

    logger.info(s"created spending tx $txHash")

    val broadcast = peerGroup.broadcastTransaction(spendingTx)

    val promise = Promise[String]

    broadcast.setProgressCallback((progress: Double) => {
      logger.info(s"tx $txHash broadcast for at ${progress * 100}%")
      if (progress == 1.0) {
        promise.completeWith(Future.successful(txHash))
      }
    })

    promise.future
  }

  private def p2pubKeyHash(value: Long, to: Address): ByteString = {

    //Create custom script containing offer's id bytes
    //      val scriptOpReturn = new ScriptBuilder().op(OP_RETURN).data("hello".getBytes()).build()

    ByteString.copyFrom(new TransactionOutput(
      networkParams,
      null,
      Coin.valueOf(value),
      to
    ).getScriptBytes)
  }

  private def isDust(satoshis: Long) = satoshis >= Transaction.MIN_NONDUST_OUTPUT.getValue

  private def outputsForOffer(offer: Offer): List[Protos.Output] = {
    def outputBuilder = Protos.Output.newBuilder

    //  if(isDust(offer.price))
    //    throw new IllegalArgumentException(s"Price ${offer.price} is too low, considered dust")

    val ownerOutput =
      outputBuilder
        .setAmount(offer.price)
        .setScript(p2pubKeyHash(
          value = offer.price,
          to = receivingAddress
        ))
        .build

    List(ownerOutput)
  }

}

