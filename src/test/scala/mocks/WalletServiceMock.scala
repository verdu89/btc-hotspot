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

package mocks

import commons.Helpers.FutureOption
import org.bitcoin.protocols.payments.Protos
import org.bitcoin.protocols.payments.Protos.{Payment, PaymentACK, PaymentRequest}
import protocol.domain
import protocol.domain.Session
import wallet.WalletServiceInterface

import scala.concurrent.Future

class WalletServiceMock extends WalletServiceInterface {
  override def generatePaymentRequest(session: Session, offerId: Long): Future[PaymentRequest] = ???
  
  override def validateBIP70Payment(payment: Protos.Payment): FutureOption[Protos.PaymentACK] = ???
  
  override def getBalance(): Long = ???
  
  override def getTransactions(): Seq[domain.BitcoinTransaction] = ???
  
  override def createSpendingTx(address: String, value: Long): Future[String] = ???
}
