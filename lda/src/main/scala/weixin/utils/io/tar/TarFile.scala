package weixin.utils.io.tar

import weixin.utils.io.seekable.RandomAccessStream


class TarFile(private[tar] val input: RandomAccessStream) {
  private[tar] def blockSize = PosixHeader.DEFAULT_RECORD_SIZE

  def entryIterator: Iterator[TarEntry] = new TarFileEntryIterator(this)

  def reader = new TarFileReader(this)
}
