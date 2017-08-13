package zukaufen.http

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import org.slf4j.LoggerFactory
import zukaufen.errors.ClientError

import scala.util.Try


/** Finagle HTTP filter, turns exceptions to HTTP error messages */
object ErrorsFilter extends SimpleFilter[Request, Response] {

  private lazy val logger = LoggerFactory.getLogger(getClass)

  override def apply(request: Request,
                     service: Service[Request, Response]): Future[Response] = {
    Try {
      service(request).rescue {
        case cl: ClientError =>
          logger.error(s"Client error inside service: $cl")
          val response = Response(Status(400))
          response.setContentString(cl.toString)
          Future(response)
        case err: Throwable =>
          logger.error(s"Server error inside service: $err")
          val response = Response(Status(500))
          response.setContentString(err.toString)
          Future(response)
      }
    }.recover {
      case cl: ClientError =>
        logger.error(s"Client error: $cl")
        Future(Response(Status(400)))
      case err: Throwable =>
        logger.error(s"Server error: $err")
        Future(Response(Status(500)))
    }.get
  }
}
