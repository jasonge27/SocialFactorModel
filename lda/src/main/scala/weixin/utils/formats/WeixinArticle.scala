package weixin.utils.formats


class WeixinArticle private(
  val date: String,
  val text: String
)

object WeixinArticle {
  def apply(content: String) = {
    val document = xml
      .parsing
      .ConstructingParser
      .fromSource(scala.io.Source.fromString(content), preserveWS = false)
      .document()
    require(document ne null)
    new WeixinArticle(
      date = (document \ "jsArticle" \ "metaList" \ "postDate").text,
      text = (document \ "jsArticle" \ "jsContent" \ "text").text
    )
  }
}
