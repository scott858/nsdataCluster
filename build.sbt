lazy val commonSettings = Seq(
  version := "1.0",
  scalaVersion := "2.11.8"
)

//resolvers += "Spark Packages Repo" at "https://dl.bintray.com/spark-packages/maven"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

//resolvers += "Sonatype (releases)" at "https://oss.sonatype.org/content/repositories/releases/"
//scalacOptions += "-Ylog-classpath"

val spark = Seq(
    ("org.apache.spark" %% "spark-core" % "2.0.2")
    .exclude("org.mortbay.jetty", "servlet-api")
    .exclude("commons.beanutils", "commons-beanutils-core")
    .exclude("commons-collections", "commons-collections")
    .exclude("commons-logging", "commons-logging")
    .exclude("com.esotericsoftware.minlog", "minlog")
)

val sparkSql = "org.apache.spark" %% "spark-sql" % "2.0.2" //intransitive()
val sparkStreaming = "org.apache.spark" % "spark-streaming_2.11" % "2.0.2" //intransitive()
//val jedis = "redis.clients" % "jedis" % "2.8.1" //intransitive()
val cassandraThrift = "org.apache.cassandra" % "cassandra-thrift" % "3.9" intransitive()
val cassandraClientUtil = "org.apache.cassandra" % "cassandra-clientutil" % "3.9" intransitive()
val cassandraCore = "com.datastax.cassandra" % "cassandra-driver-core" % "3.1.3" intransitive()
//val sparkConnector = "com.datastax.spark" %% "spark-cassandra-connector" % "1.6.0"
//val sparkConnector = "com.datastax.spark" %% "spark-cassandra-connector" % "1.6.3"
//val sparkConnector = "com.datastax.spark" %% "spark-cassandra-connector" % "1.5.2"
//val sparkConnector = "com.datastax.spark" %% "spark-cassandra-connector" % "2.0.0-M3"
val sparkConnector = "com.datastax.spark" %% "spark-cassandra-connector" % "2.0.0-M2"
val protobuf = "com.trueaccord.scalapb" %% "compilerplugin" % "0.5.47"
//val jzmq = "org.zeromq" % "jzmq" % "3.1.0"
//val jzmq = "org.zeromq" % "jzmq" % "2.1.2"
//val zeromq = "org.zeromq" % "zeromq-scala-binding_2.11.0-M3" % "0.0.7"
//val zeromq = "com.mdialog" %% "scala-zeromq" % "1.1.1"
//val akka = "com.typesafe.akka" %% "akka-actor" % "2.4.16"
//val joda = "joda-time" % "joda-time" % "2.9.7"
val sparkzmq = "org.apache.bahir" %% "spark-streaming-zeromq" % "2.0.2"
//val akka = "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.16"
//val jna = "net.java.dev.jna" % "jna" % "3.5.2"

lazy val app = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "nsdataCluster",
    libraryDependencies ++= spark,
//    libraryDependencies += sparkConnector,
    libraryDependencies += sparkSql,
    libraryDependencies += sparkStreaming,
    libraryDependencies += cassandraThrift,
    libraryDependencies += cassandraClientUtil,
    libraryDependencies += cassandraCore,
    libraryDependencies += sparkConnector,
//    libraryDependencies += jedis
//    libraryDependencies += jzmq,
//    libraryDependencies += akka
      libraryDependencies += sparkzmq,
//      libraryDependencies += akka
//    libraryDependencies += zeromq,
//    libraryDependencies += jna
    libraryDependencies += protobuf
  )

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs@_*) => MergeStrategy.last
  case PathList("javax", "activation", xs@_*) => MergeStrategy.last
  case PathList("javax", "inject", xs@_*) => MergeStrategy.last
  case PathList("org", "apache", xs@_*) => MergeStrategy.last
  case PathList("com", "google", xs@_*) => MergeStrategy.last
  case PathList("com", "esotericsoftware", xs@_*) => MergeStrategy.last
  case PathList("com", "codahale", xs@_*) => MergeStrategy.last
  case PathList("com", "yammer", xs@_*) => MergeStrategy.last
  case PathList("com", "driver", xs@_*) => MergeStrategy.last
  case PathList("io", "netty", xs@_*) => MergeStrategy.last
  case PathList("org", "aopalliance", xs@_*) => MergeStrategy.last
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
