import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

import sbt._
import Keys._
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream, TarConstants}
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.io.FileUtils


object SparkPackager extends AutoPlugin {

  val packageSpark = TaskKey[File]("package-spark", "SparkPackager jar classpath")

  private def pyquote(s: String): String = {
    if (s matches "^[A-Za-z0-9._/-]*$") {
      s"'$s'"
    } else {
      val encoded = java.util.Base64.getEncoder.encodeToString(s getBytes StandardCharsets.UTF_8)
      s"base64.b64decode('$encoded').decode('utf-8')"
    }
  }

  override def projectSettings: Seq[_root_.sbt.Def.Setting[_]] = Seq(
    packageSpark in Compile <<= (
      crossTarget in Compile,
      packageBin in Compile,
      externalDependencyClasspath in Runtime,
      streams in Compile
      ) map { case (targetValue, packageBinValue, cpValue, streamsValue) =>

      val outputFile = new File(targetValue, "package-spark.tar.gz")
      val taros = new TarArchiveOutputStream(
        new GzipCompressorOutputStream(
          new FileOutputStream(
            outputFile
          )
        )
      )
      try {
        val basedir = "package-spark"
        val jarBasedirName = s"jars"

        val jarFilesSeq = (
          packageBinValue +: cpValue.map(_.data).sorted
          ).zipWithIndex.map { case (jar, index) =>
          val jarFilename = jar.getName
          assert(jarFilename endsWith ".jar")
          val path = s"$jarBasedirName/${jarFilename stripSuffix ".jar"}.index-$index.jar"
          jar -> path
        }

        val mainJarPath = jarFilesSeq.head._2

        val script =
          s"""#!/usr/bin/env python
              |import base64
              |import inspect
              |import os
              |import sys
              |
              |cwd = os.getcwd()
              |def getpath(path):
              |    rel_basedir = os.path.relpath(basedir, cwd)
              |    return os.path.normpath(os.path.join(rel_basedir, path))
              |
              |def main():
              |    opt = sys.argv[1]
              |    if opt == '--jars':
              |        print(",".join(getpath(path) for path in all_jars))
              |    elif opt == '--main-jar':
              |        print(getpath(main_jar))
              |    else:
              |        raise Exception('invalid arguments')
              |
              |basedir = os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
              |main_jar = ${pyquote(mainJarPath)}
              |all_jars = [
              |${jarFilesSeq.map { case (_, path) => s"    ${pyquote(path)},\n" }.foldLeft("")(_ + _)}
              |]
              |
              |if __name__ == "__main__":
              |    main()
              |
          """.stripMargin.linesIterator.map(_ + "\n").reduce(_ + _)
        val scriptBytes = script getBytes StandardCharsets.US_ASCII

        taros.putArchiveEntry {
          val entry = new TarArchiveEntry(s"$basedir/get-config")
          entry.setMode(0755)
          entry.setSize(scriptBytes.length)
          entry
        }
        taros.write(scriptBytes)
        taros.closeArchiveEntry()

        taros.putArchiveEntry{
          val entry = new TarArchiveEntry(s"$basedir/$jarBasedirName", TarConstants.LF_DIR)
          entry.setMode(0755)
          entry
        }
        taros.closeArchiveEntry()

        jarFilesSeq.foreach { case (jar, path) =>
          val entry = taros.createArchiveEntry(jar, s"$basedir/$path").asInstanceOf[TarArchiveEntry]
          assert(!entry.isDirectory)
          entry.setMode(0644)
          taros.putArchiveEntry(entry)
          FileUtils.copyFile(jar, taros)
          taros.closeArchiveEntry()
        }

        streamsValue.log.info(s"${packageSpark.key.label}: generated file ${outputFile.getAbsolutePath}")
        outputFile
      }
      catch {
        case e: Throwable =>
          outputFile.deleteOnExit()
          taros.close()
          outputFile.delete()
          throw e
      }
      finally taros.close()
    }
  )

}
