package weixin.utils.formats

import java.net.URL

import org.apache.commons.io.IOUtils


case class StockBasics(entries: Seq[StockBasics.Entry])


object StockBasics {

  case class Entry(name: String, code: String)

  def loadFromResource(name: String, encoding: String = null): StockBasics = {
    loadFromURL(this.getClass.getClassLoader.getResource(name), encoding)
  }

  def loadFromURL(url: URL, encoding: String = null): StockBasics = {
    require(url ne null)
    val stream = url.openStream()
    require(stream ne null)
    try {
      val content = IOUtils.toString(stream, Option(encoding).getOrElse("UTF-8"))
      parse(content)
    }
    finally stream.close()
  }

  def parse(content: String): StockBasics = {
    parse(content.lines)
  }

  private val expectedHeader: String = "code,name,industry,area,pe,outstanding,totals,totalAssets,liquidAssets,fixedAssets,reserved,reservedPerShare,esp,bvps,pb,timeToMarket"

  private def parse(lines: Iterator[String]): StockBasics = {
    require(lines.hasNext)
    val header = lines.next()
    require(header == expectedHeader)
    val entries = lines.map { line =>
      val Array(code, name, _) = line.split(",", 3)
      Entry(name = name, code = code)
    }.toArray
    StockBasics(entries)
  }
}

