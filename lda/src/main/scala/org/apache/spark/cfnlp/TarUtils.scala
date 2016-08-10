package org.apache.spark.cfnlp

import org.apache.spark.SparkContext
import org.apache.spark.cfnlp.rdd.SingleTarContentRDD

object TarUtils {

  def tarRDD(sc: SparkContext, path: String) = new SingleTarContentRDD(sc, path)

  def tarsRDD(sc: SparkContext, paths: Seq[String]) =
    sc.union(paths.map(path => tarRDD(sc, path)))

}
