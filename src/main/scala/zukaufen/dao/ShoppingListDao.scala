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


case class ShoppingList(_id: ObjectId = new ObjectId,
                        ts: DateTime = Utils.dateTimeNow,
                        items: List[String] = List.empty,
                        name: String,
                        users: List[String]) extends MongoBaseObject


/** Mongo data access object for shopping lists */
trait ShoppingListDao extends BaseDAOMethods[ShoppingList, ObjectId] {

  def createList(name: String, uid: String, ts: Option[DateTime] = None): String = {
    val list = ShoppingList(name = name, users = List(uid), ts = ts.getOrElse(Utils.dateTimeNow))
    save(list)
    list._id.toString
  }

  def editList(id: String, name: String, ts: Option[DateTime] = None): Unit = {
    updateList(id, { _ => $set("name" -> name, "ts" -> ts.getOrElse(Utils.dateTimeNow)) })
  }

  def addItem(listId: String, itemId: String): Unit = {
    updateList(listId, { list => $set("items" -> (itemId +: list.items)) })
  }

  def removeItem(listId: String, itemId: String): Unit = {
    updateList(listId, { list => $set("items" -> list.items.filterNot(_ == itemId)) })
  }

  def addUser(id: String, uid: String): Unit = {
    updateList(id, { list => $set("users" -> (uid +: list.users)) })
  }

  def getLists(uid: String): List[ShoppingList] = {
    find(MongoDBObject("users" -> uid)).toList
  }

  private def updateList(id: String, updateObject: ShoppingList => MongoDBObject): Unit = {
    findOneById(new ObjectId(id)) match {
      case Some(list) =>
        update(
          q = MongoDBObject("_id" -> new ObjectId(id)),
          o = updateObject(list),
          upsert = false,
          multi = false,
          wc = WriteConcern.ACKNOWLEDGED)
      case _ =>
        throw new RuntimeException(s"Can't find shopping list $id")
    }
  }
}


trait ShoppingListDaoComponent {

  def shoppingListDao: ShoppingListDao = ShoppingListDaoImplementation

  object ShoppingListDaoImplementation
    extends SalatDAO[ShoppingList, ObjectId](MongoDbConfigObject.collection("lists"))
      with ShoppingListDao

}
