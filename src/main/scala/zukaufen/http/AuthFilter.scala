package zukaufen.http

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import org.slf4j.LoggerFactory
import zukaufen.auth.UserStoreClientComponent
import zukaufen.errors.AuthException
import zukaufen.utils.HttpUtils


/** Finagle HTTP filter that allows only authorized requests */
class AuthFilter extends SimpleFilter[Request, Response] with UserStoreClientComponent{

  private lazy val logger = LoggerFactory.getLogger(getClass)

  override def apply(request: Request,
                     service: Service[Request, Response]): Future[Response] = {

    (HttpUtils.getUid(request), HttpUtils.getToken(request)) match {
      case (None, _) =>
        val msg = "Can't find UID in header"
        logger.warn(msg)
        throw new AuthException(msg)

      case (_, None) =>
        val msg = "Can't find token in header"
        logger.warn(msg)
        throw new AuthException(msg)

      case (Some(uid), Some(token)) if userStoreClient.isUserLoggedIn(uid, token) =>
        service(request)

      case (Some(uid), Some(token)) =>
        val msg = s"Authentication fail for UID $uid, token $token"
        logger.warn(msg)
        throw new AuthException(msg)
    }
  }
}