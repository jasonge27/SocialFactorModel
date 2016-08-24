package weixin.utils.formats

import java.net.URL

import org.apache.commons.io.IOUtils

import java.util.regex.{Pattern, Matcher}

/**
  * Created by jian on 8/22/16.
  */
case class TopicBasics(entries: Seq[TopicBasics.Entry])

object TopicBasics{
  case class Entry(topicName: String, keywords: Seq[String], weights: Seq[Double])

  // How to factor out these two load functions? They're also used in StockBasics
  def loadFromResource(name: String, encoding: String = null): TopicBasics= {
    loadFromURL(this.getClass.getClassLoader.getResource(name), encoding)
  }

  def loadFromURL(url: URL, encoding: String = null): TopicBasics = {
    require(url ne null)
    val stream = url.openStream()
    require(stream ne null)
    try {
      val content = IOUtils.toString(stream, Option(encoding).getOrElse("UTF-8"))
      parse(content)
    }
    finally stream.close()
  }

  def parse(content: String): TopicBasics = {
    parse(content.lines)
  }

  // "keyword(0.1)" => ("keyword", 0.1)
  // "keyword" => ("keyword", 0.0)
  private def seperate_keyword_weight(str: String): (String, Double) ={
    val kwd = Pattern.compile("^[^(]+").matcher(str)
    val kwdFound = kwd.find()

    val weight = Pattern.compile("\\(([^)]+)\\)").matcher(str)
    if (weight.find()){
      (kwd.group(0), weight.group(1).toDouble)
    } else {
      (str, 0.0)
    }
  }

  def parse(lines: Iterator[String]): TopicBasics= {
    require(lines.hasNext)
    val entries = lines.map { line =>
      val Array(topicName, keywordDescription) = line.split(",", 2)
      val (keywords, weights) = keywordDescription.split(",").map(seperate_keyword_weight).unzip
      Entry(topicName, keywords, weights)
    }.toArray
    TopicBasics(entries)
  }
}

