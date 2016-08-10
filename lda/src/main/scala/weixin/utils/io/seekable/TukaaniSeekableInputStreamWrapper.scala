package weixin.utils.io.seekable

import java.io.{FilterInputStream, IOException}

import org.tukaani.xz.{SeekableInputStream => TukaaniSeekableInputStream}


class TukaaniSeekableInputStreamWrapper(stream: TukaaniSeekableInputStream)
  extends FilterInputStream(stream)
    with SeekableInputStream
{
  @throws[IOException]
  override def length: Long = stream.length()

  @throws[IOException]
  override def position: Long = stream.position()

  @throws[IOException]
  override def seek(pos: Long): Unit = stream.seek(pos)
}
