package weixin.utils.formats


case class WeixinArticle(
  dateText: String,
  mainText: String,
  title: String
) {
  def key = (dateText, title)
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
    new WeixinArticle(
      dateText = dateText,
      mainText = (document \ "jsArticle" \ "jsContent" \ "text").text,
      title = (document \ "jsArticle" \ "title").text
    )
  }
}
