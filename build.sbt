import sbt.Keys._

val scalaVersion_ = "2.11.8"
val breezeVersion = "0.11.2"


val nd4jVersion = "0.4-rc3.10"
val dl4jVersion = "0.4-rc3.10"

val stanfordParserVersion = "3.6.0"

val akkaVersion = "2.4.7"

val automataLibVersion = "0.6.0"

val sparkVersion = "1.6.2"

val commonSettings = Seq(
  version := "1.0",
  scalaVersion := scalaVersion_,
  updateOptions := updateOptions.value.withCachedResolution(true)
)

val commonDependencies = Seq(
  libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.5",

  // http://mvnrepository.com/artifact/commons-io/commons-io
  libraryDependencies += "commons-io" % "commons-io" % "2.5",

  libraryDependencies += "org.apache.commons" % "commons-compress" % "1.11",

  // http://mvnrepository.com/artifact/org.tukaani/xz
  libraryDependencies += "org.tukaani" % "xz" % "1.5",

  libraryDependencies += "org.scodec" %% "scodec-core" % "1.10.2",

  libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion,

  libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.12.0",

  libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.1.4",

  libraryDependencies += "org.scalaz.stream" %% "scalaz-stream" % "0.8",

  libraryDependencies += "org.apache.lucene" % "lucene-core" % "6.1.0",

  // libraryDependencies += "org.apache.hadoop" % "hadoop-client" % "2.7.2" exclude("javax.servlet", "servlet-api"),

  libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion,
    "org.apache.spark" %% "spark-streaming" % sparkVersion,
    "org.apache.spark" %% "spark-mllib" % sparkVersion,
    "org.apache.spark" %% "spark-graphx" % sparkVersion
  ),

  libraryDependencies += "com.huaban" % "jieba-analysis" % "1.0.2",

  libraryDependencies ++= Seq(
    // other dependencies here
    "org.scalanlp" %% "breeze" % breezeVersion,
    // native libraries are not included by default. add this if you want them (as of 0.7)
    // native libraries greatly improve performance, but increase jar sizes.
    // It also packages various blas implementations, which have licenses that may or may not
    // be compatible with the Apache License. No GPL code, as best I know.
    "org.scalanlp" %% "breeze-natives" % breezeVersion
    // the visualization library is distributed separately as well.
    // It depends on LGPL code.
    //"org.scalanlp" %% "breeze-viz" % breezeVersion
  ),

  libraryDependencies ++= Seq(
    //"org.deeplearning4j" % "deeplearning4j-core" % dl4jVersion,
    //"org.deeplearning4j" % "deeplearning4j-nlp" % dl4jVersion,
    //"org.deeplearning4j" % "deeplearning4j-ui" % dl4jVersion,
    //"org.nd4j" % "nd4j" % nd4jVersion
  ),

  libraryDependencies ++= Seq(
    "edu.stanford.nlp" % "stanford-corenlp" % stanfordParserVersion,
    "edu.stanford.nlp" % "stanford-corenlp" % stanfordParserVersion classifier "models",
    "edu.stanford.nlp" % "stanford-corenlp" % stanfordParserVersion classifier "models-chinese",

    // http://mvnrepository.com/artifact/com.google.protobuf/protobuf-java
    "com.google.protobuf" % "protobuf-java" % "2.6.1"
    )
)

lazy val cfnlpAnalysis = (project in file("."))
  .settings(commonSettings)
  .aggregate(lda)

lazy val lda = (project in file("lda"))
  .settings(commonSettings)
  .settings(commonDependencies)