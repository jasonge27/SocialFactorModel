/**
  * Created by jian on 8/7/16.
  */

package weixin.utils

import java.io.File

import weixin.utils.formats.{AShareHistory, StockBasics, TopicBasics}
import weixin.utils.io.TarContents
import weixin.utils.stocks.StockMatcher
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.IOUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

object Local {
  val weixinArticleArchiveDir = if (System.getProperty("os.name").startsWith("Windows")) {
    "C:\\Users\\guo\\x\\cfnlp\\storage\\weixin-data\\"
  } else if (System.getProperty("os.name").startsWith("Mac")) {
    "/User/jian/ChineseNLP/storage/tars/"
  } else {
      "/tigress/jiange/projs/lda/test_tars/"
  }

  lazy val weixinArticleArchiveFile = new File(weixinArticleArchiveDir, "200.f.tar.xz").getPath
  //val weixinArticleArchiveFile =

  //val weixinArticleArchiveContents = TarContents(weixinArticleArchiveFile)

  private val  resourcePath = this.getClass.getPackage.getName.replace(".", "/")
  println(resourcePath)

  val stockBasics = StockBasics.loadFromResource(resourcePath + "/stock_basics.csv")
  //lazy val stockMatcher = StockMatcher(stockBasics)
  val topicBasics = TopicBasics.loadFromResource(resourcePath + "/topic_basics.txt")

  lazy val aShareHistory: AShareHistory = {
    val path = resourcePath + "/Ashare_hist.json.gz"
    val resource = this.getClass.getClassLoader.getResourceAsStream(path).ensuring(_ ne null)
    try {
      val decompressed = new GzipCompressorInputStream(resource, true)
      try {
        val content = IOUtils.toString(decompressed, "ascii")
        AShareHistory.parse(content)
      }
      finally decompressed.close()
    }
    finally resource.close()
  }

  lazy val sparkConf = new SparkConf()
    .setAppName("cfnlp")
   // .setMaster("local[32]")
    //.set("spark.streaming.backpressure.enabled", "true")

  def sparkContext = SparkContext.getOrCreate(sparkConf)

  def streamingContext = StreamingContext.getActiveOrCreate(() => new StreamingContext(sparkContext, Seconds(1)))
}
