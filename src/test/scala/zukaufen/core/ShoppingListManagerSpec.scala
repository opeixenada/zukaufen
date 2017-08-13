package zukaufen.core

import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.internal.matchers._
import org.scalatest.{FunSpec, Matchers}
import zukaufen.dao.{ShoppingList, _}
import zukaufen.dto.{DiffItemDto, DiffListDto}
import zukaufen.utils.Utils


class ShoppingListManagerSpec extends FunSpec with Matchers {

  import ShoppingListManagerSpec._

  describe("mergeList") {
    val uid = "uid"

    val listId = new ObjectId()

    val itemId1 = new ObjectId()
    val itemId2 = new ObjectId()

    val ts1 = Utils.dateTimeNow
    val ts2 = ts1.plusSeconds(1)
    val ts3 = ts2.plusSeconds(1)

    val item1 = Item(
      _id = itemId1,
      ts = ts2,
      name = "item1",
      qty = None,
      checked = false
    )

    val item2 = item1.copy(_id = itemId2, name = "item2")

    val list = ShoppingList(
      _id = listId,
      ts = ts2,
      items = List(itemId1.toString, itemId2.toString),
      name = "list",
      users = List.empty)

    it("should modify list if change is recent") {
      val shoppingListDao: ShoppingListDao = mock(classOf[ShoppingListDao])
      when(shoppingListDao.findOneById(ArgumentMatchers.eq(listId))).thenReturn(Option(list))

      val itemDao: ItemDao = mock(classOf[ItemDao])

      val shoppingListManager = getShoppingListManager(shoppingListDao, itemDao)

      val diffListDto = DiffListDto(
        id = Some(listId.toString),
        name = "list_modified",
        items = List.empty,
        deleted = false,
        ts = ts3)

      shoppingListManager.mergeList(uid, diffListDto)

      verify(shoppingListDao, times(0)).removeById(
        ArgumentMatchers.any(),
        ArgumentMatchers.any())

      verify(shoppingListDao, times(1)).editList(
        argThat(new Equals(diffListDto.id.get)).asInstanceOf[String],
        argThat(new Equals(diffListDto.name)).asInstanceOf[String],
        argThat(new Equals(Some(diffListDto.ts))).asInstanceOf[Option[DateTime]])
    }

    it("shouldn't modify list if change is old") {
      val shoppingListDao: ShoppingListDao = mock(classOf[ShoppingListDao])
      when(shoppingListDao.findOneById(ArgumentMatchers.eq(listId))).thenReturn(Option(list))

      val itemDao: ItemDao = mock(classOf[ItemDao])

      val shoppingListManager = getShoppingListManager(shoppingListDao, itemDao)

      val diffListDto = DiffListDto(
        id = Some(listId.toString),
        name = "list_modified",
        items = List.empty,
        deleted = false,
        ts = ts1)

      shoppingListManager.mergeList(uid, diffListDto)

      verify(shoppingListDao, times(0)).removeById(
        ArgumentMatchers.any(),
        ArgumentMatchers.any())

      verify(shoppingListDao, times(0)).editList(
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
    }

    it("should delete list") {
      val shoppingListDao: ShoppingListDao = mock(classOf[ShoppingListDao])
      when(shoppingListDao.findOneById(ArgumentMatchers.eq(listId))).thenReturn(Option(list))

      val itemDao: ItemDao = mock(classOf[ItemDao])

      val shoppingListManager = getShoppingListManager(shoppingListDao, itemDao)

      val diffListDto = DiffListDto(
        id = Some(listId.toString),
        name = "list",
        items = List.empty,
        deleted = true,
        ts = ts3)

      shoppingListManager.mergeList(uid, diffListDto)

      verify(shoppingListDao, times(1)).removeById(
        ArgumentMatchers.any(),
        ArgumentMatchers.any())

      verify(itemDao, times(1)).removeById(
        argThat(new Equals(itemId1)).asInstanceOf[ObjectId],
        ArgumentMatchers.any())

      verify(itemDao, times(1)).removeById(
        argThat(new Equals(itemId2)).asInstanceOf[ObjectId],
        ArgumentMatchers.any())
    }

    it("should create new list") {
      val shoppingListDao: ShoppingListDao = mock(classOf[ShoppingListDao])
      val itemDao: ItemDao = mock(classOf[ItemDao])
      val shoppingListManager = getShoppingListManager(shoppingListDao, itemDao)

      val diffListDto = DiffListDto(
        id = None,
        name = "list",
        items = List.empty,
        deleted = false,
        ts = ts3)

      shoppingListManager.mergeList(uid, diffListDto)

      verify(shoppingListDao, times(1)).createList(
        argThat(new Equals(diffListDto.name)).asInstanceOf[String],
        argThat(new Equals(uid)).asInstanceOf[String],
        argThat(new Equals(Option(diffListDto.ts))).asInstanceOf[Option[DateTime]])
    }

    it("should modify items if changes are recent") {
      val shoppingListDao: ShoppingListDao = mock(classOf[ShoppingListDao])
      when(shoppingListDao.findOneById(ArgumentMatchers.eq(listId))).thenReturn(Option(list))

      val itemDao: ItemDao = mock(classOf[ItemDao])
      when(itemDao.findOneById(ArgumentMatchers.eq(itemId1))).thenReturn(Option(item1))
      when(itemDao.findOneById(ArgumentMatchers.eq(itemId2))).thenReturn(Option(item2))

      val shoppingListManager = getShoppingListManager(shoppingListDao, itemDao)

      val diffItemDto1 = DiffItemDto(
        id = Some(itemId1.toString),
        name = "item1_modified",
        checked = false,
        qty = None,
        deleted = false,
        ts = ts3
      )

      val diffItemDto2 = diffItemDto1.copy(id = Some(itemId2.toString), name = "item2_modified")

      val diffListDto = DiffListDto(
        id = Some(listId.toString),
        name = "list",
        items = List(diffItemDto1, diffItemDto2),
        deleted = false,
        ts = ts1)

      shoppingListManager.mergeList(uid, diffListDto)

      verify(itemDao, times(0)).removeById(
        ArgumentMatchers.any(),
        ArgumentMatchers.any())

      verify(itemDao, times(1)).updateItem(
        argThat(new Equals(diffItemDto1.id.get)).asInstanceOf[String],
        argThat(new Equals(diffItemDto1.name)).asInstanceOf[String],
        argThat(new Equals(diffItemDto1.qty)).asInstanceOf[Option[String]],
        argThat(new Equals(diffItemDto1.checked)).asInstanceOf[Boolean],
        argThat(new Equals(diffItemDto1.ts)).asInstanceOf[DateTime])

      verify(itemDao, times(1)).updateItem(
        argThat(new Equals(diffItemDto2.id.get)).asInstanceOf[String],
        argThat(new Equals(diffItemDto2.name)).asInstanceOf[String],
        argThat(new Equals(diffItemDto2.qty)).asInstanceOf[Option[String]],
        argThat(new Equals(diffItemDto2.checked)).asInstanceOf[Boolean],
        argThat(new Equals(diffItemDto2.ts)).asInstanceOf[DateTime])
    }

    it("shouldn't modify items if changes are old") {
      val shoppingListDao: ShoppingListDao = mock(classOf[ShoppingListDao])
      when(shoppingListDao.findOneById(ArgumentMatchers.eq(listId))).thenReturn(Option(list))

      val itemDao: ItemDao = mock(classOf[ItemDao])
      when(itemDao.findOneById(ArgumentMatchers.eq(itemId1))).thenReturn(Option(item1))
      when(itemDao.findOneById(ArgumentMatchers.eq(itemId2))).thenReturn(Option(item2))

      val shoppingListManager = getShoppingListManager(shoppingListDao, itemDao)

      val diffItemDto1 = DiffItemDto(
        id = Some(itemId1.toString),
        name = "item1_modified",
        checked = false,
        qty = None,
        deleted = false,
        ts = ts1
      )

      val diffItemDto2 = diffItemDto1.copy(id = Some(itemId2.toString), name = "item2_modified")

      val diffListDto = DiffListDto(
        id = Some(listId.toString),
        name = "list",
        items = List(diffItemDto1, diffItemDto2),
        deleted = false,
        ts = ts1)

      shoppingListManager.mergeList(uid, diffListDto)

      verify(itemDao, times(0)).removeById(
        ArgumentMatchers.any(),
        ArgumentMatchers.any())

      verify(itemDao, times(0)).updateItem(
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any())
    }

    it("should delete items") {
      val shoppingListDao: ShoppingListDao = mock(classOf[ShoppingListDao])
      when(shoppingListDao.findOneById(ArgumentMatchers.eq(listId))).thenReturn(Option(list))

      val itemDao: ItemDao = mock(classOf[ItemDao])
      when(itemDao.findOneById(ArgumentMatchers.eq(itemId1))).thenReturn(Option(item1))
      when(itemDao.findOneById(ArgumentMatchers.eq(itemId2))).thenReturn(Option(item2))

      val shoppingListManager = getShoppingListManager(shoppingListDao, itemDao)

      val diffItemDto1 = DiffItemDto(
        id = Some(itemId1.toString),
        name = "item1",
        checked = false,
        qty = None,
        deleted = true,
        ts = ts3
      )

      val diffItemDto2 = diffItemDto1.copy(id = Some(itemId2.toString), name = "item2")

      val diffListDto = DiffListDto(
        id = Some(listId.toString),
        name = "list",
        items = List(diffItemDto1, diffItemDto2),
        deleted = false,
        ts = ts1)

      shoppingListManager.mergeList(uid, diffListDto)

      verify(itemDao, times(1)).removeById(
        argThat(new Equals(itemId1)).asInstanceOf[ObjectId],
        ArgumentMatchers.any())

      verify(itemDao, times(1)).removeById(
        argThat(new Equals(itemId2)).asInstanceOf[ObjectId],
        ArgumentMatchers.any())
    }

    it("should create new items") {
      val shoppingListDao: ShoppingListDao = mock(classOf[ShoppingListDao])
      val itemDao: ItemDao = mock(classOf[ItemDao])
      val shoppingListManager = getShoppingListManager(shoppingListDao, itemDao)

      val diffItemDto1 = DiffItemDto(
        id = None,
        name = "item1",
        checked = false,
        qty = None,
        deleted = false,
        ts = ts3
      )

      val diffItemDto2 = diffItemDto1.copy(name = "item2")

      val diffListDto = DiffListDto(
        id = Some(listId.toString),
        name = "list",
        items = List(diffItemDto1, diffItemDto2),
        deleted = false,
        ts = ts1)

      shoppingListManager.mergeList(uid, diffListDto)

      verify(itemDao, times(1)).createItem(
        argThat(new Equals(diffItemDto1.name)).asInstanceOf[String],
        argThat(new Equals(diffItemDto1.qty)).asInstanceOf[Option[String]],
        argThat(new Equals(diffItemDto1.checked)).asInstanceOf[Boolean],
        argThat(new Equals(Some(diffItemDto1.ts))).asInstanceOf[Option[DateTime]])

      verify(itemDao, times(1)).createItem(
        argThat(new Equals(diffItemDto2.name)).asInstanceOf[String],
        argThat(new Equals(diffItemDto2.qty)).asInstanceOf[Option[String]],
        argThat(new Equals(diffItemDto2.checked)).asInstanceOf[Boolean],
        argThat(new Equals(Some(diffItemDto2.ts))).asInstanceOf[Option[DateTime]])
    }
  }
}


object ShoppingListManagerSpec {

  def getShoppingListManager(shoppingListDaoImpl: ShoppingListDao,
                             itemDaoImpl: ItemDao): ShoppingListManager =
    new ShoppingListManager with ShoppingListDaoComponent with ItemDaoComponent {
      override val shoppingListDao: ShoppingListDao = shoppingListDaoImpl
      override val itemDao: ItemDao = itemDaoImpl
    }

}