package weixin.utils.io.tar

import java.io.{Closeable, InputStream}
import java.nio.charset.{Charset, StandardCharsets}

import org.apache.commons.io.IOUtils
import org.apache.commons.io.input.BoundedInputStream


class TarFileReader(tarFile: TarFile) extends Closeable {
  val stream = tarFile.input.open

  override def close(): Unit = stream.close()

  def read[T](entry: TarEntry)(reader: InputStream => T): T = synchronized {
    require(entry.tarFile eq tarFile)
    stream.seek(entry.offset)
    reader(new BoundedInputStream(stream, entry.size))
  }

  def getByteArray(entry: TarEntry) = read(entry) { is =>
    val buffer = new Array[Byte](entry.size.toInt)
    IOUtils.readFully(is, buffer)
    buffer
  }

  def getString(entry: TarEntry, charset: Charset = StandardCharsets.UTF_8) = read(entry) { is =>
    IOUtils.toString(is, charset)
  }
}
