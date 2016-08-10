package weixin.utils.io.seekable

import java.io.{IOException, InputStream}


trait SeekableInputStream extends InputStream {
  @throws[IOException]
  override def skip(n: Long): Long = {
    if (n <= 0) return 0
    val size: Long = length
    val pos: Long = position
    if (pos >= size) return 0
    val actualSize = if (size - pos < n)
      size - pos
    else
      n
    seek(pos + actualSize)
    actualSize
  }

  @throws[IOException]
  def length: Long

  @throws[IOException]
  def position: Long

  @throws[IOException]
  def seek(pos: Long): Unit
}
