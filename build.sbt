lazy val commonSettings = Seq(
  version := "1.0",
  scalaVersion := "2.11.8"
)

packAutoSettings

updateOptions := updateOptions.value.withCachedResolution(true)

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

val spark = Seq(
  ("org.apache.spark" %% "spark-core" % "2.1.0")
    .exclude("org.mortbay.jetty", "servlet-api")
    .exclude("commons.beanutils", "commons-beanutils-core")
    .exclude("commons-collections", "commons-collections")
    .exclude("commons-logging", "commons-logging")
    .exclude("com.esotericsoftware.minlog", "minlog")
)

lazy val app = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "nsdataCluster",
    libraryDependencies ++= spark,
    libraryDependencies += "com.datastax.spark" %% "spark-cassandra-connector" % "2.0.0-M2",
    libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.1.0",
    libraryDependencies += "org.apache.spark" % "spark-streaming_2.11" % "2.1.0",
    libraryDependencies += "org.apache.cassandra" % "cassandra-thrift" % "3.9" intransitive(),
    libraryDependencies += "org.apache.cassandra" % "cassandra-clientutil" % "3.9" intransitive(),
    libraryDependencies += "com.datastax.cassandra" % "cassandra-driver-core" % "3.1.3" intransitive(),
    libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.5.47",
    libraryDependencies += "org.zeromq" % "jeromq" % "0.3.5",
    libraryDependencies += "com.mdialog" %% "scala-zeromq" % "1.1.1",
    libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.16.0"
  )

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
assemblyMergeStrategy in assembly := {
  case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
  case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  //  case PathList("javax", "servlet", xs@_*) => MergeStrategy.last
  case _ => MergeStrategy.first
}

