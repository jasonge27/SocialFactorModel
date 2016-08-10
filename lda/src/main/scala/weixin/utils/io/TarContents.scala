package weixin.utils.io

import java.io.{FileInputStream, InputStream}
import java.nio.charset.Charset

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream, BZip2Utils}
import org.apache.commons.compress.compressors.xz.{XZCompressorInputStream, XZUtils}
import org.apache.commons.io.IOUtils


class TarIterator(stream: InputStream, charset: Charset) extends Iterator[TarContents.Entry] with java.io.Closeable {
  def this(stream: InputStream) = this(stream, java.nio.charset.StandardCharsets.UTF_8)

  private var as = new TarArchiveInputStream(stream)

  private var currentEntry: ArchiveEntry = _

  def close(): Unit = {
    if (as != null) {
      as.close()
      as = null
    }
    currentEntry = null
  }

  private def shift(): Unit = if (as != null) {
    do {
      currentEntry = as.getNextEntry
    } while (currentEntry != null && currentEntry.isDirectory)
    if (currentEntry == null) {
      close()
    }
  }

  shift()

  override def hasNext: Boolean = currentEntry ne null

  override def next(): TarContents.Entry = {
    val name = currentEntry.getName
    val s = IOUtils.toString(as, charset)
    shift()
    TarContents.Entry(name = name, content = s)
  }
}


private class TarContents(path: String) extends Iterable[TarContents.Entry] {
  private def stream: InputStream = {
    var s: InputStream = new FileInputStream(path)
    if (XZUtils.isCompressedFilename(path))
      s = new XZCompressorInputStream(s, true)
    else if (BZip2Utils.isCompressedFilename(path))
      s = new BZip2CompressorInputStream(s, true)
    s
  }

  override def iterator: Iterator[TarContents.Entry] = new TarIterator(stream)
}

object TarContents {
  case class Entry(name: String, content: String)

  def nameAndContent(path: String): Iterable[Entry] = new TarContents(path)

  def apply(path: String): Iterable[String] = new Iterable[String] {
    override def iterator: Iterator[String] = nameAndContent(path).iterator.map(_.content)
  }
}
