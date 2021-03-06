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

package protocol

import com.typesafe.scalalogging.LazyLogging
import commons.Configuration.DbConfig._
import commons.Helpers
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

trait DatabaseComponent extends LazyLogging {

  val database: DatabaseImpl

  class DatabaseImpl {

    val database = {
      logger.info(s"Opening database for conf '$configPath' @ $jdbcUrl")

      if (webUI) {
        logger.info(s"Creating web ui @ localhost:8888")
        org.h2.tools.Server.createWebServer("-webAllowOthers", "-webPort", "8888").start()
      }

      DatabaseConfig.forConfig[JdbcProfile](configPath)
    }

    def db = database.db

    Helpers.addShutDownHook {
      logger.info("Shutting down db")
      Await.result(db.shutdown, Duration(2, "seconds"))
    }
  }

}