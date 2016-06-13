val spark = "org.apache.spark" %% "spark-core" % "1.6.0"
val sparkSql = "org.apache.spark" %% "spark-sql" % "1.6.0" intransitive()
val cassandraThrift = "org.apache.cassandra" % "cassandra-thrift" % "3.5" intransitive()
val cassandraClientUtil = "org.apache.cassandra" % "cassandra-clientutil" % "3.5" intransitive()
val cassandraCore = "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.2"
val sparkConnector = "com.datastax.spark" %% "spark-cassandra-connector" % "1.6.0"

lazy val commonSettings = Seq(
  version := "1.0",
  scalaVersion := "2.11.8"
)

lazy val app = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "hello",
    libraryDependencies += sparkConnector,
    libraryDependencies += spark,
    libraryDependencies += sparkSql,
    libraryDependencies += cassandraThrift,
    libraryDependencies += cassandraClientUtil,
    libraryDependencies += cassandraCore
  )
