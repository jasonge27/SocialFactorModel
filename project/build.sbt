
val root = (project in file("."))
  .settings(
    libraryDependencies += "commons-io" % "commons-io" % "2.5",
    libraryDependencies += "org.apache.commons" % "commons-compress" % "1.11"
  )
