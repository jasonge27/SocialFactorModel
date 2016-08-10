package weixin.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import weixin.utils.formats.WeixinArticle
import weixin.utils.io.TarContents

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object ParseWeixinArticle {
  def parse(content: String): Unit ={
    WeixinArticle(content)
  }

  def main(args: Array[String]) {
    val contents = TarContents(Local.weixinArticleArchiveFile)
    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()
    val r = Source.fromIterator(() => contents.iterator).async.runForeach(parse)
    Await.ready(r, Duration.Inf)
    Await.ready(system.terminate(), Duration.Inf)
  }
}
