package zukaufen.utils

import com.twitter.finagle.http.Message
import org.json4s.ext.JodaTimeSerializers
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.LoggerFactory
import zukaufen.errors.JsonParseException

import scala.util.control.NonFatal


object HttpUtils {

  private lazy val logger = LoggerFactory.getLogger(getClass)

  val X_AUTH_TOKEN = "X-Auth-Token"
  val UserId = "User-Id"

  def getToken(message: Message): Option[String] = message.headerMap.get(X_AUTH_TOKEN)

  def getUid(message: Message): Option[String] = message.headerMap.get(UserId)

  lazy val defaultFormats: Formats = DefaultFormats ++ JodaTimeSerializers.all

  def parseJsonFromHttpMessage[T](message: Message)(
    implicit mt: scala.reflect.Manifest[T], format: Formats = defaultFormats): T = {
    val body = message.contentString
    try {
      parse(body).extract[T]
    } catch {
      case NonFatal(e) =>
        logger.warn(s"${e.getClass} ${e.getMessage}: can't parse JSON from $body")
        throw new JsonParseException(e.getMessage)
    }
  }

}

