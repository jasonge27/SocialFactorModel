package weixin.lda

import com.huaban.analysis.jieba.JiebaSegmenter.SegMode
import weixin.utils.formats.WeixinArticle
import weixin.utils.{DistributedUtils, JiebaUtils, Local}
import org.apache.spark.mllib.clustering.LDA
import org.apache.spark.mllib.linalg.{SparseVector, Vector}
import org.apache.spark.rdd.RDD
import org.apache.spark.cfnlp.TarUtils

import java.util.Date

import weixin.utils.formats.{StockBasics, TopicBasics}

import scala.collection.convert.decorateAll._
import scala.collection.mutable
import scalaz.syntax.id._

object PlayGround {

  private def createSegmenterFunction = () => JiebaUtils.createJiebaSegmenter

  def stockTopicLinksCalculation(paths: Seq[String]) {
    println(paths)
    val sc = Local.sparkContext

    val stockBasics = Local.stockBasics
    val topicBasics = Local.topicBasics

    // val stockBasicsBroadcasted = sc.broadcast(Local.stockBasics)
    // val topicBasicsBroadcasted = sc.broadcast(Local.topicBasics)


    val articleRDD: RDD[WeixinArticle] = {
      TarUtils.tarsRDD(sc, paths)
        .map(_.content)
        //.zipWithUniqueId().map(_.swap)
        //.repartition(100)
        .map(raw => WeixinArticle(raw))
    }

    //val articleCount: Long = articleRDD.count()


    val articleRDDNonDup: RDD[WeixinArticle] = articleRDD
      .map(article => (article.key, article))
      .reduceByKey((a, b) => a)
      .values
    articleRDDNonDup.persist()

    //val articleRDDNonDup = articleRDD


    val articleCountNonDup = articleRDDNonDup.count()

    articleRDDNonDup.map(item => (item.date, item.title)).take(30).foreach(println)

    //println(s"Total number of articles: ${articleCount}")
    println(s"Num of articles after pruning: $articleCountNonDup")



    def processArticle(stockName: String, keywords: Seq[String])(article:WeixinArticle) : (Date, Double) = {
      val dateParser = new java.text.SimpleDateFormat("yyyy-MM-dd")
      val articleSentences = article.maintext.split("。|！|，|,|\\.|!|\n").map(_.trim).filter(_.length > 0)

      val buf = scala.collection.mutable.ListBuffer.empty[Seq[String]]

      var i = 0
      if (articleSentences.length >= 3){
        for (i <- articleSentences.indices) {
          if (i + 2 < articleSentences.length) {
            buf += Seq(articleSentences(i), articleSentences(i+1), articleSentences(i+2))
          }
        }
      } else {
        buf += articleSentences
      }

      val sentenceList = buf.toList

      val occurrence = sentenceList.filter { localList =>
        localList
          .filter(_.contains(stockName))
          .exists { item => keywords.forall(item.contains) }
      }

      if (occurrence.nonEmpty){
        (article.date, 1.0)
      } else {
        (article.date, 0.0)
      }
    }

    case class StockTopicLink(stockName: String, topicName: String, date: Date)

    val stockBasicsRDD = sc.parallelize(stockBasics.entries)
    val topicBasicsRDD = sc.parallelize(topicBasics.entries)

    val stockTopicLink =
      articleRDDNonDup.cartesian(stockBasicsRDD.cartesian(topicBasicsRDD))
        .map { case (article, (stock, topic)) =>
          val (date, score) = processArticle(stock.name, topic.keywords)(article)
          (StockTopicLink(stock.name, topic.topicName, date), score)
        }
      .reduceByKeyLocally(_ + _)
      .toMap

    stockTopicLink.foreach(println)

    //val wordsCount: Seq[(String, Long)] = articleRDD.flatMap(_._2).countByValue().toSeq.sortBy(-_._2)

    //val words: Seq[String] = wordsCount.map(_._1).take(100)

    //println(s"words.size: ${words.size}")
    //println("top words:")
    //words.take(30).foreach(println)

    /*
    val wordIndex: Map[String, Int] = words.zipWithIndex.toMap

    val wordIndexBroadcasted = sc.broadcast(wordIndex)

    val articleVectors: RDD[(Long, Vector)] = {
      val wordCount = words.size
      val localWordIndexBroadcasted = wordIndexBroadcasted
      articleRDD
        .mapValues { doc =>
          val localWordIndex = localWordIndexBroadcasted.value
          val counter = mutable.Map.empty[Int, Long].withDefaultValue(0L)
          for {
            w <- doc
            index <- localWordIndex.get(w)
          }
            counter.update(index, counter(index) + 1L)
          val countArray = counter.toArray
          new SparseVector(
            size = wordCount,
            indices = countArray.map(_._1),
            values = countArray.map(_._2.toDouble)
          )
        }
    }
    articleVectors.persist()

    val ldaModel = new LDA().setK(20).run(articleVectors)
    println("Learned topics (as distributions over vocab of " + ldaModel.vocabSize + " words):")

    val topics = ldaModel.topicsMatrix
    for (topic <- Range(0, ldaModel.k)) {
      print("Topic " + topic + ":")
      for (word <- Range(0, ldaModel.vocabSize)) { print(" " + topics(word, topic)); }
      println()
    }
    */

  }

  def main(args: Array[String]) {
    val start = System.nanoTime()
    "Hello world!" |> println
    stockTopicLinksCalculation(Seq(Local.weixinArticleArchiveFile))
    val end = System.nanoTime()
    println(s"time spent: ${(end - start) / 1e9}")
  }
}


