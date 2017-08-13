package zukaufen.core

import org.bson.types.ObjectId
import org.joda.time.DateTime
import zukaufen.dao.{Item, ItemDaoComponent, ShoppingList, ShoppingListDaoComponent}
import zukaufen.dto.{DiffItemDto, DiffListDto}


/** Holds methods to access and modify shopping lists and items */
class ShoppingListManager extends ShoppingListDaoComponent with ItemDaoComponent {

  def getLists(uid: String): List[ShoppingList] = shoppingListDao.getLists(uid)

  def createList(listName: String, uid: String, ts: Option[DateTime] = None): String =
    shoppingListDao.createList(listName, uid, ts)

  def editList(listId: String, listName: String, ts: Option[DateTime] = None): Unit =
    shoppingListDao.editList(listId, listName, ts)

  def addUser(listId: String, uid: String): Unit = shoppingListDao.addUser(listId, uid)

  def deleteList(listId: String): Unit = {
    shoppingListDao.findOneById(new ObjectId(listId)) match {
      case Some(list) =>
        val items = list.items
        shoppingListDao.removeById(new ObjectId(listId))
        items.foreach { itemId => itemDao.removeById(new ObjectId(itemId)) }
      case _ =>
    }
  }

  def getItem(itemId: String): Option[Item] = itemDao.findOneById(new ObjectId(itemId))

  def createItem(itemName: String,
                 listId: String,
                 qty: Option[String],
                 checked: Boolean,
                 ts: Option[DateTime] = None): String = {
    val itemId = itemDao.createItem(itemName, qty, checked, ts)
    shoppingListDao.addItem(listId, itemId)
    itemId
  }

  def editQty(itemId: String, qty: Option[String], ts: Option[DateTime] = None): Unit =
    itemDao.editQty(itemId, qty, ts)

  def editChecked(itemId: String, checked: Boolean, ts: Option[DateTime] = None): Unit =
    itemDao.editChecked(itemId, checked, ts)

  def deleteItem(listId: String, itemId: String): Unit = {
    shoppingListDao.removeItem(listId, itemId)
    itemDao.removeById(new ObjectId(itemId))
  }

  def mergeList(uid: String, diffListDto: DiffListDto): Unit = {
    val listId: String = diffListDto.id match {

      case Some(id) =>
        // list already exists on backend

        shoppingListDao.findOneById(new ObjectId(id)) match {
          case Some(_) if diffListDto.deleted =>
            // list should be deleted
            deleteList(id)

          case Some(list) if list.ts.isBefore(diffListDto.ts) =>
            // update is recent, apply it
            editList(id, diffListDto.name, Some(diffListDto.ts))

          case _ => // update is not relevant, ignore it

        }

        id

      case _ =>
        // list is new, create it on backend
        createList(
          listName = diffListDto.name,
          uid = uid,
          ts = Some(diffListDto.ts)
        )
    }


    diffListDto.items.foreach { syncItemDto => mergeItem(listId, syncItemDto) }
  }

  private def mergeItem(listId: String, diffItemDto: DiffItemDto): Unit = {
    diffItemDto.id match {

      case Some(id) =>
        // item already exists on backend

        itemDao.findOneById(new ObjectId(id)) match {
          case Some(_) if diffItemDto.deleted =>
            // item should be deleted
            deleteItem(listId, id)

          case Some(item) if item.ts.isBefore(diffItemDto.ts) =>
            // update is recent, apply it
            itemDao.updateItem(id, diffItemDto.name, diffItemDto.qty,
              diffItemDto.checked, diffItemDto.ts)

          case _ => // update is not relevant, ignore it
        }

      case _ =>
        // item is new, create it
        createItem(
          itemName = diffItemDto.name,
          listId = listId,
          qty = diffItemDto.qty,
          checked = diffItemDto.checked,
          ts = Some(diffItemDto.ts))
    }
  }
}


trait ShoppingListManagerComponent {

  def shoppingListManager: ShoppingListManager = ShoppingListManagerImpl

  object ShoppingListManagerImpl extends ShoppingListManager

}