import sbt.Keys._

val scalaVersion_ = "2.11.8"

val sparkVersion = "2.0.0"

val commonSettings = Seq(
  version := "1.0",
  scalaVersion := scalaVersion_,
  updateOptions := updateOptions.value.withCachedResolution(true)
)

val sparkDependencies = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion
)

val commonDependencies = Seq(
  libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.5",

  libraryDependencies += "commons-io" % "commons-io" % "2.5",

  libraryDependencies += "org.apache.commons" % "commons-compress" % "1.11",

  libraryDependencies += "org.tukaani" % "xz" % "1.5",

  libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.12.0",

  libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.1.4",

  libraryDependencies ++= sparkDependencies map (_ % Provided),

  libraryDependencies += "com.huaban" % "jieba-analysis" % "1.0.2"

)

lazy val lda = (project in file("lda"))
  .enablePlugins(SparkPackager)
  .settings(commonSettings)
  .settings(commonDependencies)

lazy val ldaRunner = (project in file("ldaRunner"))
  .settings(commonSettings)
  .dependsOn(lda)
  .settings(
    libraryDependencies ++= sparkDependencies map (_ % Compile)
  )
