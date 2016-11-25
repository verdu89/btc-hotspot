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

package resources

import akka.http.scaladsl.server.Route
import commons.JsonSupport
import protocol.Repository
import protocol.webDto.WebOfferDto

/**
  * Created by andrea on 13/11/16.
  */
trait OffersAPI extends CommonResource with JsonSupport {


  def offersRoute:Route = get {
    path("api" / "offers"){
      complete(Repository.allOffers.map(WebOfferDto(_)))
    }
  }

}
