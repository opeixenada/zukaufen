package zukaufen.dao

import com.mongodb.WriteConcern
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.{BaseDAOMethods, SalatDAO}
import com.novus.salat.global.ctx
import org.bson.types.ObjectId
import org.joda.time.DateTime
import zukaufen.mongo.{MongoBaseObject, MongoDbConfigObject}
import zukaufen.utils.Utils


case class Item(_id: ObjectId = new ObjectId,
                ts: DateTime = Utils.dateTimeNow,
                name: String,
                qty: Option[String],
                checked: Boolean) extends MongoBaseObject


/** Mongo data access object for shopping items */
trait ItemDao extends BaseDAOMethods[Item, ObjectId] {

  def createItem(name: String,
                 qty: Option[String] = None,
                 checked: Boolean = false,
                 ts: Option[DateTime] = None): String = {
    val item = Item(
      ts = ts.getOrElse(Utils.dateTimeNow),
      name = name,
      checked = checked,
      qty = qty)
    save(item)
    item._id.toString
  }

  def editName(id: String, name: String, ts: Option[DateTime] = None): Unit = {
    updateItem(id, { _ =>
      $set("name" -> name, "ts" -> ts.getOrElse(Utils.dateTimeNow))
    })
  }

  def editQty(id: String, qty: Option[String], ts: Option[DateTime] = None): Unit = {
    updateItem(id, { _ =>
      $set("qty" -> qty, "ts" -> ts.getOrElse(Utils.dateTimeNow))
    })
  }

  def editChecked(id: String, checked: Boolean, ts: Option[DateTime] = None): Unit = {
    updateItem(id, { _ =>
      $set("checked" -> checked, "ts" -> ts.getOrElse(Utils.dateTimeNow))
    })
  }

  def updateItem(id: String,
                 name: String,
                 qty: Option[String],
                 checked: Boolean,
                 ts: DateTime): Unit = {
    updateItem(id, { _ =>
      $set("checked" -> checked, "ts" -> ts, "qty" -> qty, "name" -> name)
    })
  }

  private def updateItem(id: String, updateObject: Item => MongoDBObject): Unit = {
    findOneById(new ObjectId(id)) match {
      case Some(item) =>
        update(
          q = MongoDBObject("_id" -> new ObjectId(id)),
          o = updateObject(item),
          upsert = false,
          multi = false,
          wc = WriteConcern.ACKNOWLEDGED)
      case _ =>
        throw new NoSuchElementException(s"Can't find item $id")
    }
  }
}


trait ItemDaoComponent {

  def itemDao: ItemDao = ItemDaoImplementation

  object ItemDaoImplementation
    extends SalatDAO[Item, ObjectId](MongoDbConfigObject.collection("items")) with ItemDao

}
