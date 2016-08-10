package weixin.utils.io.seekable

import java.io._


class RandomAccessFileWrapper(randomAccessFile: RandomAccessFile) extends SeekableInputStream {
  override def close(): Unit = randomAccessFile.close()

  override def length: Long = randomAccessFile.length()

  override def position: Long = randomAccessFile.getFilePointer

  override def seek(pos: Long): Unit = randomAccessFile.seek(pos)

  override def read(): Int = randomAccessFile.read()

  override def read(b: Array[Byte], off: Int, len: Int): Int = randomAccessFile.read(b, off, len)
}
