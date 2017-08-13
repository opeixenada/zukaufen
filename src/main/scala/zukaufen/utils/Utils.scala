package zukaufen.utils

import org.joda.time.{DateTime, DateTimeZone}


object Utils {

  def dateTimeNow: DateTime = DateTime.now(DateTimeZone.UTC)

}
