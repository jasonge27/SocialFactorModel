package weixin.utils

import weixin.utils.io.TarContents


object CountArchive {
  def apply(path: String): Int = TarContents(path).size

  def main(args: Array[String]): Unit = {
    Local.weixinArticleArchiveFileNames.map(apply).foreach(println)
  }
}
