package weixin.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{StreamingContext, Time}
import org.apache.spark.streaming.dstream.{DStream, InputDStream}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scalaz.stream.{Process, Process0}


object ParallelUtils {
  implicit lazy val system = ActorSystem()

  def akkaParallelForeach[T](input: Iterator[T])(f: T => Unit) = {
    implicit val mat = ActorMaterializer()
    val fut = Source.fromIterator(() => input).async.runForeach(f)
    Await.ready(fut, Duration.Inf)
  }

  def scalazFromIterator[T](input: Iterator[T]): Process0[T] = {
    Process.unfold(input) { i =>
      if (i.hasNext) Some((i.next, i))
      else None
    }
  }

  def scalazParallelForeach[T](input: Iterator[T])(f: T => Unit) = {
    scalazFromIterator(input).map(f).toSource.runLast.run
  }

  def iteratorToDStream[T: ClassTag](ssc: StreamingContext, iterator: Iterator[Seq[T]]): DStream[T] = {
    new InputDStream[T](ssc) {
      override def start(): Unit = {}

      override def stop(): Unit = {}

      override def compute(validTime: Time): Option[RDD[T]] = {
        if (iterator.hasNext)
          Some(ssc.sparkContext.makeRDD(iterator.next))
        else
          None
      }
    }
  }
}
