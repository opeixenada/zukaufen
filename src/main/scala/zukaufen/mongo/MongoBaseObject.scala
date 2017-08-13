package zukaufen.mongo

import com.novus.salat.annotations._
import org.bson.types.ObjectId


@Salat
trait MongoBaseObject {
  def _id: ObjectId
  def id: String = _id.toString
}

