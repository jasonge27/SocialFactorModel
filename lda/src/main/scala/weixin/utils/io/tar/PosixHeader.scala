package weixin.utils.io.tar


import java.nio.charset.StandardCharsets

import scalaz.syntax.id._


// http://www.gnu.org/software/tar/manual/html_node/Standard.html
/*
struct posix_header
{                              /* byte offset */
  char name[100];               /*   0 */
  char mode[8];                 /* 100 */
  char uid[8];                  /* 108 */
  char gid[8];                  /* 116 */
  char size[12];                /* 124 */
  char mtime[12];               /* 136 */
  char chksum[8];               /* 148 */
  char typeflag;                /* 156 */
  char linkname[100];           /* 157 */
  char magic[6];                /* 257 */
  char version[2];              /* 263 */
  char uname[32];               /* 265 */
  char gname[32];               /* 297 */
  char devmajor[8];             /* 329 */
  char devminor[8];             /* 337 */
  char prefix[155];             /* 345 */
                                /* 500 */
};
 */
case class PosixHeader(record: Array[Byte]) {
  require(record.length % 512 == 0)

  private def s(offset: Int, size: Int) = record.slice(offset, offset + size)

  private def str(offset: Int, size: Int): String = {
    for (pos <- 0 until size) {
      if (record(offset + pos) == 0)
        return new String(record, offset, pos, StandardCharsets.US_ASCII)
    }
    throw new Exception("nil not found in bytes for string")
  }

  private def oct(offset: Int, size: Int) = str(offset, size) |> (Integer.parseUnsignedInt(_, 8))

  private def byte(offset: Int) = record(offset)

  val name = str(0, 100)
  val mode = oct(100, 8)
  val uid = oct(108, 8)
  val gid = oct(116, 8)
  val size = oct(124, 12)
  val mtime = oct(136, 12)
  val chksum = oct(136, 12)
  val typeflag = byte(156)
  val linkname = str(157, 100)
  val magic = str(257, 6)
  require(magic == "ustar")
  val version = s(263, 2)
  require(version.length == 2 && version(0) == '0' && version(1) == '0')
  val uname = str(265, 32)
  val gname = str(297, 32)
  val devmajor = oct(329, 8)
  val devminor = oct(337, 8)
  val prefix = str(345, 155)
}

object PosixHeader {
  val DEFAULT_RECORD_SIZE = 512
}
