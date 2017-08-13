package zukaufen.dto

import org.joda.time.DateTime


case class DiffListDto(id: Option[String],
                       name: String,
                       items: List[DiffItemDto],
                       deleted: Boolean,
                       ts: DateTime)


case class DiffItemDto(id: Option[String],
                       name: String,
                       qty: Option[String],
                       checked: Boolean,
                       deleted: Boolean,
                       ts: DateTime)