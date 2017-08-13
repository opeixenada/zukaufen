package zukaufen

import com.twitter.conversions.time._
import com.twitter.finagle.Http
import com.twitter.util.Await
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import zukaufen.http.{AuthFilter, ErrorsFilter, ListsService}


object Main {

  private lazy val logger = LoggerFactory.getLogger(getClass)

  private def configureHttpServer(server: Http.Server, config: Config): Http.Server = {
    val requestTimeout = config.getInt("finagle.requestTimeout")
    server.withRequestTimeout(requestTimeout.seconds)
  }

  def main(args: Array[String]): Unit = {

    logger.info("Running server...")

    val config = ConfigFactory.load()
    val host = config.getString("http.host")
    val port = config.getInt("http.port")

    logger.info(s"Bind host: $host, port: $port")

    val authFilter = new AuthFilter()
    val listsService = new ListsService(config)
    val serviceChain = ErrorsFilter andThen authFilter andThen listsService

    val server = configureHttpServer(Http.server, config).serve(s"$host:$port", serviceChain)

    Await.ready(server)
  }
}
