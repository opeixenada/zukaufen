package zukaufen.mongo

import com.mongodb.casbah.commons.conversions.scala._
import com.mongodb.casbah.{MongoClient, MongoClientURI, MongoCollection}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory


object MongoDbConfigObject {

  private lazy val logger = LoggerFactory.getLogger(getClass)

  logger.debug("MongoDbConfigObject initialization")

  private val conf = ConfigFactory.load.getConfig("mongo")
  private val host = System.getProperties.getProperty("mongo.host")
  private val uri = s"mongodb://$host/${conf.getString("db")}"
  private val mongoDbUri: MongoClientURI = MongoClientURI(uri)

  private lazy val mongoDb = {
    logger.debug(s"MongoDb uri: $mongoDbUri")
    MongoClient(mongoDbUri)
  }

  private lazy val mongoConnection = {
    logger.debug(s"Mongo database: ${mongoDbUri.database.get}")

    RegisterConversionHelpers()
    RegisterJodaTimeConversionHelpers()

    mongoDb(mongoDbUri.database.get)
  }

  def collection(name: String): MongoCollection = {
    logger.debug(s"Collection name: $name")
    val collection = mongoConnection(name)
    logger.debug("Connection OK")
    collection
  }

}
