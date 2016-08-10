package weixin.utils.io.seekable


trait RandomAccessStream {

  def open: SeekableInputStream

  def map(f: SeekableInputStream => SeekableInputStream): RandomAccessStream =
    RandomAccessStream.withOpen {
      f(open)
    }
}

object RandomAccessStream {
  def withOpen(open: => SeekableInputStream) = {
    def open_ = open
    new RandomAccessStream {
      override def open: SeekableInputStream = open_
    }
  }
}
