package weixin.lda

import com.huaban.analysis.jieba.JiebaSegmenter.SegMode
import weixin.utils.formats.WeixinArticle
import weixin.utils.{DistributedUtils, JiebaUtils, Local}
import org.apache.spark.mllib.clustering.LDA
import org.apache.spark.mllib.linalg.{SparseVector, Vector}
import org.apache.spark.rdd.RDD
import org.apache.spark.cfnlp.TarUtils

import scala.collection.convert.decorateAll._
import scala.collection.mutable
import scalaz.syntax.id._

object PlayGround {

  private def createSegmenterFunction = () => JiebaUtils.createJiebaSegmenter

  def ldaDemo(paths: Seq[String]) {
    println(paths)
    val sc = Local.sparkContext

    val articleRDD: RDD[(Long, Array[String])] = {
      val segmenterPool = sc.broadcast(DistributedUtils.LocalPool(createSegmenterFunction, 16))
      TarUtils.tarsRDD(sc, paths)
        .map(_.content)
        .zipWithUniqueId().map(_.swap)
        .repartition(100)
        .mapPartitions { contentIter =>
          segmenterPool.value.on { seg =>
            contentIter.map { case (index, raw) =>
              val document = WeixinArticle(raw).text
              val words = seg.process(document, SegMode.INDEX).asScala.view.map(_.word).toArray
              index -> words
            }
          }
        }
    }

    val wordsCount: Seq[(String, Long)] = articleRDD.flatMap(_._2).countByValue().toSeq.sortBy(-_._2)

    val words: Seq[String] = wordsCount.map(_._1).take(100)

    println(s"words.size: ${words.size}")
    println("top words:")
    words.take(30).foreach(println)

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
  }

  def main(args: Array[String]) {
    val start = System.nanoTime()
    "Hello world!" |> println
    ldaDemo(Seq(Local.weixinArticleArchiveFile))
    val end = System.nanoTime()
    println(s"time spent: ${(end - start) / 1e9}")
  }
}


