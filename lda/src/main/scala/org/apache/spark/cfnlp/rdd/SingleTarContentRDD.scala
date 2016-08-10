package org.apache.spark.cfnlp.rdd

import java.io.InputStream

import weixin.utils.io.TarContents.Entry
import weixin.utils.io.{TarContents, TarIterator}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.compress.CompressionCodecFactory
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.rdd.RDD
import org.apache.spark.util.SerializableConfiguration
import org.apache.spark.{InterruptibleIterator, Partition, SparkContext, TaskContext}


private[spark]
class SingleTarContentRDD(sc: SparkContext, path: String) extends RDD[TarContents.Entry](sc, Seq.empty) {
  import org.apache.hadoop.fs.{FileSystem, Path}

  private val confBroadcast = sc.broadcast(new SerializableConfiguration(sc.hadoopConfiguration))

  private def getConf: Configuration = confBroadcast.value.value

  private def decompress(fsPath: Path, stream: InputStream): InputStream = {
    val codecOpt = Option(new CompressionCodecFactory(getConf).getCodec(fsPath))

    codecOpt match {
      case Some(codec) =>
        logInfo(s"use hadoop codec: ${codec.getClass.getName} for $fsPath")
        codec.createInputStream(stream)
      case None =>
        import org.apache.commons.compress.compressors.xz
        if (xz.XZUtils.isCompressedFilename(fsPath.getName)) {
          logInfo(s"use apache common compress codec: xz for $fsPath")
          new xz.XZCompressorInputStream(stream, true)
        }
        else {
          logInfo(s"no decompress codec used for $fsPath")
          stream
        }
    }
  }

  @DeveloperApi
  override def compute(split: Partition, context: TaskContext): Iterator[Entry] = {
    require(split eq SingleTarContentRDD.ThePartition)
    val fs = FileSystem.get(getConf)
    val fsPath = new Path(path)

    val rawStream = fs.open(fsPath)
    val stream = decompress(fsPath, rawStream)

    val iter = new TarIterator(stream)
    context.addTaskCompletionListener(_ => iter.close())
    new InterruptibleIterator(context, iter)
  }

  override protected def getPartitions: Array[Partition] = Array(SingleTarContentRDD.ThePartition)
}


private[spark]
object SingleTarContentRDD {

  case object ThePartition extends Partition {
    override def index: Int = 0
  }

}
