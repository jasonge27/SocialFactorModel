package weixin.utils.io.tar


class TarEntry private[tar](
  private[tar] val tarFile: TarFile,
  val name: String,
  private[tar] val headerOffset: Long,
  private[tar] val offset: Long,
  val size: Long
)
