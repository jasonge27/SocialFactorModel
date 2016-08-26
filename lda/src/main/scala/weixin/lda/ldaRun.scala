package weixin.lda

import weixin.utils.formats.{StockBasics, TopicBasics, WeixinArticle}
import weixin.utils.Local
import org.apache.spark.rdd.RDD
import org.apache.spark.cfnlp.TarUtils
import weixin.utils.stocks.StringMatcher

import scalaz.syntax.id._

object PlayGround {

  case class StockAndTopic(stockName: String, topicName: String)

  case class TextMatcher(private val stockBasics: StockBasics, private val topicBasics: TopicBasics) {

    private trait Matched

    private case class Stock(name: String) extends Matched

    private case class Topic(name: String) extends Matched

    private type MatchedSet = Set[Matched]

    private implicit class RichMatchedSet(matchedSet: MatchedSet) {
      def stockAndTopicPairs: TraversableOnce[StockAndTopic] = {
        for {
          Stock(stockName) <- matchedSet.iterator
          Topic(topicName) <- matchedSet.iterator
        } yield StockAndTopic(stockName = stockName, topicName = topicName)
      }
    }

    @transient private lazy val map: Map[String, List[Matched]] = {
      val seq = List.empty ++
        stockBasics.entries.map { entry =>
          entry.name -> Stock(entry.name)
        } ++
        topicBasics.entries.flatMap { entry =>
          entry.keywords.map { keyword =>
            keyword -> Topic(entry.topicName)
          }
        }
      seq
        .groupBy(_._1)
        .mapValues { seq =>
          seq.map { case (_, matched) => matched }
        }
    }
    @transient private lazy val matcher = StringMatcher(map)

    private def matchText(text: String): MatchedSet = {
      matcher.query(text)
        .toIterator
        .flatMap(_._1)
        .toSet
    }

    private def matchTextSet(textSet: TraversableOnce[String]): MatchedSet =
      textSet
        .map(matchText)
        .fold(Set.empty)(_ union _)

    def matchSentencesTriple(sentences: IndexedSeq[String]): Set[StockAndTopic] = {
      val head = sentences.take(3)
      if (head.size < 3)
        matchTextSet(head).stockAndTopicPairs.toSet
      else {
        val is0 = matchText(head(0))
        val is1 = matchText(head(1))
        val is2 = matchText(head(2))
        sentences
          .iterator
          .drop(3)
          .map(matchText)
          .scanLeft((is0, is1, is2)) { case ((s0, s1, s2), s3) =>
            (s1, s2, s3)
          }
          .flatMap { case (s0, s1, s2) =>
            val s012 = s0 union s1 union s2
            s012.stockAndTopicPairs
          }
          .toSet
      }
    }
  }

  def splitSentences(text: String): Array[String] = {
    text.split("。|！|，|,|\\.|!|\n").map(_.trim).filter(_.length > 0)
  }

  def stockTopicLinksCalculation(paths: Seq[String]) {
    println("paths:", paths.toList)

    val sc = Local.sparkContext

    val articleRDD: RDD[WeixinArticle] = {
      TarUtils.tarsRDD(sc, paths)
        .map(_.content)
        //.repartition(100)
        .map(raw => WeixinArticle(raw))
    }
    //val articleCount: Long = articleRDD.count()


    val articleRDDNonDup: RDD[WeixinArticle] = articleRDD
      .map(article => (article.key, article))
      .reduceByKey((a, b) => a)
      .values
    articleRDDNonDup.persist()

    val articleCountNonDup = articleRDDNonDup.count()

    articleRDDNonDup.take(30).foreach(item => println(item.dateText, item.title))

    //println(s"Total number of articles: ${articleCount}")
    println(s"Num of articles after pruning: $articleCountNonDup")


    case class StockTopicLink(stockName: String, topicName: String, dateText: String)

    val matcher = sc.broadcast {
      TextMatcher(Local.stockBasics, Local.topicBasics)
    }

    val stockTopicLinkMap: Map[StockTopicLink, Double] =
      articleRDDNonDup
        .flatMap { article =>
          val sentences = splitSentences(article.mainText)
          val stockAndTopicPairs = matcher.value.matchSentencesTriple(sentences)
          for {
            StockAndTopic(stockName, topicName) <- stockAndTopicPairs.iterator
            link = StockTopicLink(
              stockName = stockName,
              topicName = topicName,
              dateText = article.dateText
            )
            score = 1.0
          } yield link -> score
        }
        .reduceByKeyLocally(_ + _)
        .toMap

    stockTopicLinkMap
      .toIndexedSeq
      .sortBy(_._2)
      .foreach(println)

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
    stockTopicLinksCalculation(Local.weixinArticleArchiveFileNames)
    val end = System.nanoTime()
    println(s"time spent: ${(end - start) / 1e9}")
  }
}


