package zukaufen.dto

import zukaufen.dao.{Item, ShoppingList}


case class ListDto(id: String,
                   name: String,
                   items: List[ItemDto])


object ListDto {
  def apply(list: ShoppingList, item: String => Option[Item]): ListDto = ListDto(
    id = list._id.toString,
    name = list.name,
    items = list.items.flatMap(item(_)).map(ItemDto(_)))
}