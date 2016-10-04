package resources

import akka.http.scaladsl.marshalling.GenericMarshallers
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout
import ipc.{READ_MEM, SharedStruct}
import scala.concurrent.{ExecutionContext}
import scala.concurrent.duration._

/**
  * Created by andrea on 15/09/16.
  */
trait SharedMemoryResource extends CommonResource with GenericMarshallers {

  implicit val timeout = Timeout(10 seconds)

  def route: Route = {
    get {
      path(""){
        complete {
          greetingPage
        }
      }
    }
  }

  val greetingPage =
    """
      |<HTML>
      |<HEAD>
      |<TITLE> Welcome :)</TITLE>
      |</HEAD>
      |<BODY BGCOLOR="FFFFFF">
      |
      |<HR>
      |<H1>This is a Header</H1>
      |<H2>This is a Medium Header</H2>
      |Send me mail at <a href="mailto:support@yourcompany.com">
      |support@yourcompany.com</a>.
      |<P> This is a new paragraph!
      |<P> <B>This is a new paragraph!</B>
      |<BR> <B><I>This is a new sentence without a paragraph break, in bold italics.</I></B>
      |<HR>
      |</BODY>
      |</HTML>
    """.stripMargin

}
