package me.jiahua.cfnlp.analysis.abstracts


trait Segmenter {
  def segmentString(sentence: String): Seq[String]
}
