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

package services

import commons.Helpers.FutureOption
import protocol.domain.Offer
import registry.{ OfferRepositoryRegistry }

import scala.concurrent.Future

object OfferServiceRegistry extends OfferServiceComponent {

  val offerService: OfferServiceInterface = new OfferService

}

trait OfferServiceComponent {

  val offerService: OfferServiceInterface

}

trait OfferServiceInterface {

  def allOffers: Future[Seq[Offer]]

  def offerById(id: Long): FutureOption[Offer]

}

class OfferService extends OfferServiceInterface {

  def allOffers: Future[Seq[Offer]] = {
    OfferRepositoryRegistry.offerRepositoryImpl.allOffers
  }

  def offerById(id: Long): FutureOption[Offer] = OfferRepositoryRegistry.offerRepositoryImpl.byId(id)

}
