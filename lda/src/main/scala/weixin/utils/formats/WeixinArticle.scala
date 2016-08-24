package weixin.utils.formats

import java.util.Date

class WeixinArticle (
  val date: Date,
  val maintext: String,
  val title: String
) {
  val key = (date, title)
}

object WeixinArticle {
  def apply(content: String) = {
    val document = xml
      .parsing
      .ConstructingParser
      .fromSource(scala.io.Source.fromString(content), preserveWS = false)
      .document()
    require(document ne null)
    val dateText = (document \ "jsArticle" \ "metaList" \ "postDate").text
    val dateParser = new java.text.SimpleDateFormat("yyyy-MM-dd")
    new WeixinArticle(
      date = try {
        dateParser.parse(dateText)
      } catch {
        case e:Exception => dateParser.parse("2048-00-00")
      },
      maintext = (document \ "jsArticle" \ "jsContent" \ "text").text,
      title = (document \ "jsArticle" \ "title").text
    )
  }
}
