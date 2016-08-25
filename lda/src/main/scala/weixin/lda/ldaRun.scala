package weixin.lda

import weixin.utils.formats.WeixinArticle
import weixin.utils.{JiebaUtils, Local}
import org.apache.spark.rdd.RDD
import org.apache.spark.cfnlp.TarUtils

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

    articleRDDNonDup.take(30).foreach(item => println(item.dateText, item.title))

    //println(s"Total number of articles: ${articleCount}")
    println(s"Num of articles after pruning: $articleCountNonDup")



    def calculateArticleScore(stockName: String, keywords: Seq[String], article: WeixinArticle): Double = {
      val articleSentences = article.mainText.split("。|！|，|,|\\.|!|\n").map(_.trim).filter(_.length > 0)

      case class SentenceState(containsStockName: Boolean, containsAnyKeyword: Boolean) {
        def +(other: SentenceState) = SentenceState(
          containsStockName = containsStockName || other.containsStockName,
          containsAnyKeyword = containsAnyKeyword || other.containsAnyKeyword
        )
        def matched = containsStockName && containsAnyKeyword
      }
      def getState(sentence: String) = SentenceState(
        containsStockName = sentence contains stockName,
        containsAnyKeyword = keywords exists sentence.contains
      )

      val matched = if (articleSentences.length < 3) {
        articleSentences.map(getState).reduce(_ + _).matched
      } else {
        val Array(is0, is1, is2) = articleSentences.slice(0, 2).map(getState)
        articleSentences
          .iterator
          .drop(3)
          .map(getState)
          .scanLeft((is0, is1, is2)) { case ((s0, s1, s2), s3) =>
            (s1, s2, s3)
          }
          .exists { case (s0, s1, s2) =>
            (s0 + s1 + s2).matched
          }
      }

      val score = if (matched) 1.0 else 0.0
      score
    }

    case class StockTopicLink(stockName: String, topicName: String, dateText: String)

    val stockBasicsRDD = sc.parallelize(stockBasics.entries)
    val topicBasicsRDD = sc.parallelize(topicBasics.entries)

    val stockTopicLink =
      articleRDDNonDup.cartesian(stockBasicsRDD.cartesian(topicBasicsRDD))
        .map { case (article, (stock, topic)) =>
          val score = calculateArticleScore(stock.name, topic.keywords, article)
          (StockTopicLink(stock.name, topic.topicName, article.dateText), score)
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


