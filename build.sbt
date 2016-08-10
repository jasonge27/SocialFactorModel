import sbt.Keys._

val scalaVersion_ = "2.11.8"

val sparkVersion = "1.6.2"

val commonSettings = Seq(
  version := "1.0",
  scalaVersion := scalaVersion_,
  updateOptions := updateOptions.value.withCachedResolution(true)
)

val commonDependencies = Seq(
  libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.5",

  libraryDependencies += "commons-io" % "commons-io" % "2.5",

  libraryDependencies += "org.apache.commons" % "commons-compress" % "1.11",

  libraryDependencies += "org.tukaani" % "xz" % "1.5",

  libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.12.0",

  libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.1.4",

  libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
    "org.apache.spark" %% "spark-mllib" % sparkVersion % Provided
  ),

  libraryDependencies += "com.huaban" % "jieba-analysis" % "1.0.2"

)

lazy val lda = (project in file("lda"))
  .enablePlugins(SparkPackager)
  .settings(commonSettings)
  .settings(commonDependencies)
