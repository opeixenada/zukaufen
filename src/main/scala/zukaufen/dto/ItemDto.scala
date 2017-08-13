package zukaufen.dto

import zukaufen.dao.Item


case class ItemDto(id: String,
                   name: String,
                   qty: Option[String],
                   checked: Boolean)


object ItemDto {
  def apply(item: Item): ItemDto =
    new ItemDto(item._id.toString, item.name, item.qty, item.checked)
}
