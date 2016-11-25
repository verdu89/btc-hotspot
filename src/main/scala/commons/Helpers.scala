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

package commons

import Configuration._
import akka.actor.ActorSystem
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import scala.concurrent._
import scala.reflect.ClassTag
import scala.reflect._

/**
  * Created by andrea on 21/10/16.
  */
package object Helpers {

  private def actorPathFor[T:ClassTag] = {
    s"${config.getString("akka.actorSystem")}/user/${classTag[T].runtimeClass.getSimpleName}$$"
  }

  def actorRefFor[T:ClassTag](implicit actorSystem:ActorSystem) = {
    actorSystem.actorSelection(s"akka://${actorPathFor[T]}")
  }

  object ScalaConversions {

    implicit class ListenableFutureToScalaFuture[T](lfuture:ListenableFuture[T]) {
      def toScalaFuture:Future[T] = {
        val promise = Promise[T]()
        Futures.addCallback(lfuture, new FutureCallback[T] {
          override def onFailure(t: Throwable): Unit = promise failure t
          override def onSuccess(result: T): Unit = promise success result
        })

        promise.future
      }
    }

  }


}
