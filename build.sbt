
organization := "chainkite"

name := "graylog-plugin-quill-output"

version := "1.1-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "io.getquill" %% "quill-async" % "0.9.0",
  "org.graylog2" % "graylog2-server" % "2.1.0" % "provided",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "ru.yandex.qatools.embed" % "postgresql-embedded" % "1.15" % "test"
)

fork in Test := false
parallelExecution in Test := false

//    test in assembly := {}
assemblyJarName in assembly := s"${name}-${version}.jar"
assemblyMergeStrategy in assembly := {
  case x if x.endsWith(".class") => MergeStrategy.last
  case x if x.endsWith(".properties") => MergeStrategy.last
  case x if x.contains("/resources/") => MergeStrategy.last
  case x if x.startsWith("META-INF/mailcap") => MergeStrategy.last
  case x if x.startsWith("META-INF/mimetypes.default") => MergeStrategy.first
  case x if x.startsWith("META-INF/maven/org.slf4j/slf4j-api/pom.") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    if (oldStrategy == MergeStrategy.deduplicate)
      MergeStrategy.first
    else
      oldStrategy(x)
}