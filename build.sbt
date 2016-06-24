val spark = Seq(
  ("org.apache.spark" %% "spark-core" % "1.6.1")
    .exclude("org.mortbay.jetty", "servlet-api")
    .exclude("commons.beanutils", "commons-beanutils-core")
    .exclude("commons-collections", "commons-collections")
    .exclude("commons-logging", "commons-logging")
    .exclude("com.esotericsoftware.minlog", "minlog")
)
val sparkSql = "org.apache.spark" %% "spark-sql" % "1.6.1" //intransitive()
val sparkStreaming = "org.apache.spark" % "spark-streaming_2.11" % "1.6.1" //intransitive()
val jedis = "redis.clients" % "jedis" % "2.8.1" //intransitive()
val cassandraThrift = "org.apache.cassandra" % "cassandra-thrift" % "3.5" intransitive()
val cassandraClientUtil = "org.apache.cassandra" % "cassandra-clientutil" % "3.5" intransitive()
val cassandraCore = "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.2" intransitive()
val sparkConnector = "com.datastax.spark" %% "spark-cassandra-connector" % "1.6.0"

lazy val commonSettings = Seq(
  version := "1.0",
  scalaVersion := "2.11.8"
)

lazy val app = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "sparkCassandra",
    libraryDependencies ++= spark,
    libraryDependencies += sparkConnector,
    libraryDependencies += sparkSql,
    libraryDependencies += sparkStreaming,
    libraryDependencies += cassandraThrift,
    libraryDependencies += cassandraClientUtil,
    libraryDependencies += cassandraCore,
    libraryDependencies += jedis
  )

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs@_*) => MergeStrategy.last
  case PathList("javax", "activation", xs@_*) => MergeStrategy.last
  case PathList("org", "apache", xs@_*) => MergeStrategy.last
  case PathList("com", "google", xs@_*) => MergeStrategy.last
  case PathList("com", "esotericsoftware", xs@_*) => MergeStrategy.last
  case PathList("com", "codahale", xs@_*) => MergeStrategy.last
  case PathList("com", "yammer", xs@_*) => MergeStrategy.last
  case PathList("io", "netty", xs@_*) => MergeStrategy.last
  case "about.html" => MergeStrategy.rename
  case "META-INF/ECLIPSEF.RSA" => MergeStrategy.last
  case "META-INF/mailcap" => MergeStrategy.last
  case "META-INF/mimetypes.default" => MergeStrategy.last
  case "META-INF/io.netty.versions.properties" => MergeStrategy.last
  case "plugin.properties" => MergeStrategy.last
  case "log4j.properties" => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)