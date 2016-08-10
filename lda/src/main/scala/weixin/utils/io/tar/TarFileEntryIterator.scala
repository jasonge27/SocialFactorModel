package weixin.utils.io.tar

import java.io.{Closeable, EOFException}

import org.apache.commons.io.IOUtils

import scala.util.control.NonFatal


private[tar] class TarFileEntryIterator(tarFile: TarFile) extends Iterator[TarEntry] with Closeable {
  private def blockSize = tarFile.blockSize

  private val stream = tarFile.input.open
  private val buffer = new Array[Byte](blockSize)
  private var bufferOffset: Long = -1
  private var currentEntry: TarEntry = _

  def close(): Unit = {
    bufferOffset = -1
    currentEntry = null
    stream.close()
  }

  private def aligned(pos: Long): Long = (pos + blockSize - 1) & (blockSize - 1)

  private def loadBuffer(): Boolean = {
    try {
      IOUtils.readFully(stream, buffer)
      bufferOffset = stream.position
      true
    }
    catch {
      case e: EOFException =>
        close()
        false
    }
  }

  private def loadEntry(): Unit = {
    if (!loadBuffer())
      return
    if (buffer.forall(_ == 0))
      if (!loadBuffer())
        return
    if (buffer.forall(_ == 0)) {
      close()
      return
    }

    try {
      val header = PosixHeader(buffer)
      val entry = new TarEntry(
        tarFile = tarFile,
        name = header.name,
        headerOffset = bufferOffset,
        offset = stream.position,
        size = header.size
      )
      stream.seek(aligned(entry.offset + entry.size))
      currentEntry = entry
    }
    catch {
      case NonFatal(e) =>
        close()
        throw e
    }
  }

  override def hasNext: Boolean = currentEntry != null

  override def next(): TarEntry = {
    assert(hasNext)
    val entry = currentEntry
    loadEntry()
    entry
  }

  loadEntry()
}
