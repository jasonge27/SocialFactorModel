package weixin.utils.formats

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.{DateTimeFormatterBuilder, ISODateTimeFormat}
import org.json4s.JsonAST.JValue

import scalaz.syntax.id._


case class AShareHistory(entries: Seq[AShareHistory.Entry]) {
  val tradingDays = entries.map(_.date).groupBy(_.getMillis).toArray.sortBy(_._1).map(_._2.head)
}


object AShareHistory {

  type StockID = String

  case class Entry(
    stockId: StockID,
    open: Double,
    highest: Double,
    lowest: Double,
    close: Double,
    volume: Double,
    date: DateTime
  )

  private object JsonMethods extends org.json4s.jackson.JsonMethods {
    import com.fasterxml.jackson.core.JsonParser
    mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true)
  }

  def parse(json: String): AShareHistory = {
    val jValue = JsonMethods.parse(json, useBigDecimalForDouble = false)
    parse(jValue)
  }

  private val dateTimeFormat = new DateTimeFormatterBuilder()
    .append(ISODateTimeFormat.date())
    .appendLiteral(' ')
    .append(ISODateTimeFormat.hourMinuteSecondFraction())
    .toFormatter
    .withZone(DateTimeZone.forID("Asia/Shanghai"))

  def parse(jValue: JValue): AShareHistory = {
    import org.json4s.JsonAST._
    val entries = jValue.asInstanceOf[JObject].obj
      .filter { case (stockId, elements) => stockId != "公司代码" }
      .flatMap { case (stockId, elements) =>
        elements.asInstanceOf[JArray].arr
          .ensuring(_.length == 6)
          .toArray
          .map(x => x.asInstanceOf[JArray].arr.toArray)
          .transpose
          .map { case Array(open, highest, lowest, close, volume, date) =>
            implicit val formats = org.json4s.DefaultFormats
            Entry(
              stockId = stockId,
              open = open.extract[Double],
              highest = highest.extract[Double],
              lowest = lowest.extract[Double],
              close = close.extract[Double],
              volume = volume.extract[Double],
              date = date.extract[String] |> dateTimeFormat.parseDateTime |> (_.withTimeAtStartOfDay())
            )
          }
      }
    AShareHistory(entries)
  }

}
