package tasks

/**
  * Created by scott on 6/12/16.
  */

import java.util.UUID

import bms_voltage._

//import akka.zeromq._
import java.nio.ByteBuffer

import akka.util.ByteString
import akka.zeromq.Subscribe
import com.datastax.spark.connector._
import com.datastax.spark.connector.streaming._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.streaming._
import org.apache.spark.streaming.zeromq.ZeroMQUtils
import org.apache.spark.{HashPartitioner, SparkConf, SparkContext}


object sparkTaskOld {

//  val host = "192.168.1.16"
  val host = "192.168.1.72"
  val assemblyPath = "/home/scott/repos/code/nsdataCluster/" +
    "target/scala-2.11/nsdataCluster-assembly-1.0.jar"

//    val redisHost = "172.17.0.3"
//    val redisPort = "7379"
//  val redisHost = "172.16.30.13"
//  val redisPort = "6379"

    val sparkMaster = "172.17.0.2"
//  val sparkMaster = "172.16.30.15"
  val sparkPort = "7077"

  val simpleSchema = "create table if not exists " +
    "device_data.data_stream( " +
    "device_id text, " +
    "sample_time bigint, " +
    "data_type int, " +
    "data_value float, " +
    "data_crc int, " +
    "raw_packet text, " +
    "primary key((device_id), sample_time, data_type, raw_packet)" +
    ");"

  val lessSimpleSchema = "create table if not exists " +
    "device_data.data_stream( " +
    "device_id text, " +
    "sample_time bigint, " +
    "voltage_a float, " +
    "voltage_b float, " +
    "current_b float, " +
    "temperature float, " +
    "soc float, " +
    "primary key((device_id), sample_time)" +
    ");"

  val rawSchema = "create table if not exists " +
    "device_data.data_stream( " +
    "device_id text, " +
    "sample_time bigint, " +
    "raw_packet text, " +
    "primary key((device_id), sample_time, raw_packet)" +
    ");"

  def oldmain(args: Array[String]) = {
//    runSpark()
//            debugStreamData()
    saveBmsVoltageStreamToCassandra()
  }

  def saveStreamToCassandraTest(): Unit = {
    val conf = new SparkConf(true)
      .setAppName("Streaming Example")
      .setMaster("spark://" + sparkMaster + ":" + sparkPort)
      .set("spark.executor.memory", "2G")
      .set("spark.cassandra.connection.host", sparkMaster)
      .set("spark.cassandra.output.consistency.level", "ONE")
      .set("spark.cleaner.ttl", "3600")
//      .set("redis.host", redisHost)
//      .set("redis.port", redisPort)

    val cc = com.datastax.spark.connector.cql.CassandraConnector(conf)

    cc.withSessionDo { session =>
      session.execute("use test_data")
    }

    cc.withSessionDo { session =>
      session.execute("create keyspace if not exists " +
        "test_data " +
        "with " +
        "replication = {'class': 'SimpleStrategy', 'replication_factor': 3}")
    }

    cc.withSessionDo(session =>
      session.execute("drop table if exists " +
        "fuck_you")
    )

    cc.withSessionDo { session =>
      session.execute("create table if not exists " +
        "test_data.fuck_you ( " +
        "f text, " +
        "u text, " +
        "d text, " +
        "id text, " +
        "primary key(id));")
    }

    val ssc = new StreamingContext(conf, Seconds(4))
    val stream = ssc.socketTextStream(host, 9999)

    stream.map(record => record.split("\n"))
      .map { case Array(p) => parseBmsVoltagePacket(p) }
      .saveToCassandra("test_data", "fuck_you")

    ssc.start()
    ssc.awaitTermination()
  }

  case class ParsedBmsVoltagePacket(id: String, d: String, f: String, u: String)

  def parseBmsVoltagePacket(packet: String): ParsedBmsVoltagePacket = {
    packet.split(" ") match{
      case Array(f, u, d, id) => ParsedBmsVoltagePacket(id, f, u, d)
      case _ => ParsedBmsVoltagePacket("0", "0", "0", "0")
    }
  }

  def parseBmsVoltageProtobuf(packet: Array[Byte]): BmsVoltage = {
    try{
      packet.map(x => x.toString).foreach(print)
      BmsVoltage.parseFrom(packet)
    } catch {
      case e: Exception =>
      zeroBmsVoltageBuff()
    }
  }

  def zeroBmsVoltageBuff(): BmsVoltage = {
    BmsVoltage(
      realTime = 1,
      cpuTime = 2,
      deviceId = 3,
      voltage0 = 4,
      voltage1 = 0,
      voltage2 = 0,
      voltage3 = 0,
      voltage4 = 0,
      voltage5 = 0,
      voltage6 = 0,
      voltage7 = 0,
      voltage8 = 0,
      voltage9 = 0,
      voltage10 = 0,
      voltage11 = 0,
      voltage12 = 0,
      voltage13 = 0,
      voltage14 = 0,
      voltage15 = 0
    )
  }

  def saveBmsVoltageStreamToCassandra(): Unit = {
    val conf = new SparkConf(true)
      .setAppName("Streaming Example")
      .setMaster("spark://" + sparkMaster + ":" + sparkPort)
      .set("spark.executor.memory", "2G")
      .set("spark.cassandra.connection.host", sparkMaster)
      .set("spark.cassandra.output.consistency.level", "ONE")
      .set("spark.cleaner.ttl", "3600")

    val cc = com.datastax.spark.connector.cql.CassandraConnector(conf)

    cc.withSessionDo { session =>
      session.execute("create keyspace if not exists " +
        "aerobms " +
        "with " +
        "replication = {'class': 'SimpleStrategy', 'replication_factor': 1}")
    }

    cc.withSessionDo(session => session.execute("use aerobms"))

    cc.withSessionDo(session =>
      session.execute("drop table if exists " +
        "cell_voltages")
    )

    cc.withSessionDo { session =>
      session.execute("create table if not exists " +
        "aerobms.cell_voltages ( " +
        "real_time  int, " +
        "cpu_time   int, " +
        "device_id  int, " +
        "voltage_0  int, " +
        "voltage_1  int, " +
        "voltage_2  int, " +
        "voltage_3  int, " +
        "voltage_4  int, " +
        "voltage_5  int, " +
        "voltage_6  int, " +
        "voltage_7  int, " +
        "voltage_8  int, " +
        "voltage_9  int, " +
        "voltage_10 int, " +
        "voltage_11 int, " +
        "voltage_12 int, " +
        "voltage_13 int, " +
        "voltage_14 int, " +
        "voltage_15 int, " +
        "primary key((real_time, device_id), cpu_time));")
    }

    val ssc = new StreamingContext(conf, Seconds(4))
    ssc.sparkContext.setLogLevel("DEBUG")
//    val stream = ssc.socketTextStream(host, 9999)

//    Set logging level if log4j not configured (override by adding log4j.properties to classpath)
    if (!Logger.getRootLogger.getAllAppenders.hasMoreElements) {
      Logger.getRootLogger.setLevel(Level.DEBUG)
    }

    def bytesToStringIterator(x: Seq[ByteString]): Iterator[String] =
      x.map(_.utf8String).iterator

    val stream = ZeroMQUtils.createStream(
      ssc,
      "192.168.1.72",
      Subscribe("0"),
      bytesToStringIterator _
    )

    val protobytes = stream.map(row => row.getBytes())
    protobytes.print
    val protobuf = protobytes.map(x => parseBmsVoltageProtobuf(x))
//    val protobuf = zeroBmsVoltageBuff()
//    val protobuf = protobytes.map{case Array(x) => parseBmsVoltageProtobuf(x.getBytes)}
//    ssc.sparkContext.parallelize(Seq(protobuf)).saveToCassandra("aerobms", "cell_voltages")
    protobuf.print
    protobuf.saveToCassandra("aerobms", "cell_voltages")
//      .saveToCassandra("aerobms", "cell_voltages")

//    stream.map(record => record.split("\0"))
//      .map { case Array(p) => parseBmsVoltageProtobuf(p.getBytes) }
//      .saveToCassandra("test_data", "fuck_you")

    ssc.start()
    ssc.awaitTermination()
  }

  def runSpark(): Unit = {
    val conf = new SparkConf(true)
      .setAppName("Streaming Example")
      .setMaster("spark://" + sparkMaster + ":" + sparkPort)
      .set("spark.executor.memory", "2G")
      .set("spark.cassandra.connection.host", sparkMaster)
      .set("spark.cleaner.ttl", "3600")
//      .set("redis.host", redisHost)
//      .set("redis.port", redisPort)

    val sc = new SparkContext(conf)
//    sc.setLogLevel("INFO")
    val cc = com.datastax.spark.connector.cql.CassandraConnector(conf)
    val ssc = new StreamingContext(sc, Seconds(4))
//    val redisStream = ssc.createRedisStreamWithoutListname(Array("device-log"),
//      storageLevel = StorageLevel.MEMORY_AND_DISK_SER_2)

    cc.withSessionDo { session =>
      session.execute(
        "create keyspace if not exists " +
          "device_data " +
          "with " +
          "replication = {'class': 'SimpleStrategy', 'replication_factor': 1};"
      )
    }

    cc.withSessionDo { session =>
      session.execute(
        "drop table if exists " +
          "device_data.data_stream;"
      )
    }

    cc.withSessionDo { session =>
      session.execute(
        simpleSchema
      )
    }

    cc.withSessionDo { session =>
      session.execute(
        rawSchema
      )
    }

//    redisStream.map(record => parseRecord(record))
//      .map { case Array(u, t, p) => parseRawPacket(u, t, p) }
//      .saveToCassandra("device_data", "data_stream",
//        SomeColumns("device_id", "sample_time", "data_type",
//          "data_value", "data_crc", "raw_packet"))

    ssc.start()
    ssc.awaitTermination()
  }

  def debugStreamData(): Unit = {
    //    val record = "[u'0065768c-2ee6-43eb-83c0-6125393ea9c8', 0, u'200547d703000009550603c00f']"
    val record = "[u'0065768c-2ee6-43eb-83c0-6125393ea9c8', 10, u'200547d70300000955060309550603']"
    val recordArray = parseRecord(record)
    recordArray.foreach(println)
    val parsedPacket = parseRawPacket(recordArray(0), recordArray(1), recordArray(2))
    print(parsedPacket)
  }

  def parseRecord(record: String): Array[String] = {
    val replaceThis = """[\]]|[\[]|[u]|[\']"""
    val recordArray = record.replaceAll(replaceThis, "")
      .split(",")
      .map(_.trim)

    recordArray
  }

  case class ParsedPacket(deviceId: String, sampleTime: BigInt, dataType: Int,
                          dataValue: Float, dataCrc: Int, rawPacket: String)

  def parseRawPacket(deviceId: String, sampleTime: String, packet: String): ParsedPacket = {
    if (packet.length == 26) {
      val rawPacketArray = packet.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)

      val timeArray = rawPacketArray.slice(3, 7).reverse
      val timeValue = ByteBuffer.wrap(timeArray).getInt + sampleTime.toInt

      val dataArray = rawPacketArray.slice(8, 12).reverse
      val dataValue = ByteBuffer.wrap(dataArray).getFloat

      val dataType = rawPacketArray(7)
      val crc = rawPacketArray(12) & 0xFF
      ParsedPacket(deviceId, timeValue, dataType, dataValue, crc, "")
    } else {
      ParsedPacket(deviceId, sampleTime.toInt, 0, 0, 0, packet)
    }
  }

  def streamingPackets(): Unit = {
    val conf = new SparkConf(true)
      .setAppName("Streaming Example")
      .setMaster("spark://" + sparkMaster + ":" + sparkPort)
      .set("spark.executor.memory", "2G")
      .set("spark.cassandra.connection.host", sparkMaster)
      .set("spark.cleaner.ttl", "3600")

    val sc = new SparkContext(conf)
    val ssc = new StreamingContext(sc, Seconds(4))
    val stream = ssc.socketTextStream(host, 9999)
    stream.flatMap(record => record.split(" "))
      .map(word => (word, 1))
      .reduceByKey(_ + _)
      .print()

    ssc.start()
    ssc.awaitTermination()
  }

  case class Record(addedDate: String, title: String, description: Option[String])

  case class GenresInfo(title: String, release_date: String, genres: Set[String])

  def streamJoinStream(sc: SparkContext): Unit = {
    val ssc = new StreamingContext(sc, Seconds(4))
    val stream1 = ssc.socketTextStream(host, 9999)
    val stream2 = ssc.socketTextStream(host, 9998)

    stream1.countByValue()
      .join(stream2.countByValue())
      .mapValues { case (v1, v2) => v1 + v2 }
      .print

    ssc.start()
    ssc.awaitTermination()
  }

  def statefulStreaming(sc: SparkContext): Unit = {
    val ssc = new StreamingContext(sc, Seconds(4))
    val stream = ssc.socketTextStream(host, 9999)

    def updateMovieCount(newValues: Seq[Long], oldCount: Option[Long]): Option[Long] = {
      if (newValues.isEmpty) Some(oldCount.getOrElse(0L))
      else Some(oldCount.getOrElse(0L) + newValues(0))
    }

    stream.countByValue()
      .updateStateByKey[Long](updateMovieCount _)
      .print

    ssc.checkpoint("/home")

    ssc.start()
    ssc.awaitTermination()
  }

  def streamingJoinToTable(sc: SparkContext): Unit = {
    val ssc = new StreamingContext(sc, Seconds(4))
    val stream = ssc.socketTextStream(host, 9999)
      .countByValue()

    val movies = ssc.cassandraTable("killrvideo", "videos")
      .select("video_id", "title", "release_year")
      .as((id: UUID, t: String, y: Int) => (id.toString(), (t, y)))
      .partitionBy(new HashPartitioner(2 * ssc.sparkContext.defaultParallelism))
      .cache

    stream.transform(rdd => rdd.join(movies).map { case (id, (c, (t, y))) => (id, t, y, c) })
      .print()

    ssc.start()
    ssc.awaitTermination()
  }

  def optimalRepartition(sc: SparkContext): Unit = {
    val movies = sc.cassandraTable("killr_video", "videos")
      .keyBy(row => row.getString("release_date"))
      .partitionBy(
        new org.apache.spark.HashPartitioner(2 * sc.defaultParallelism)
      )
      .cache

    val movieCountByYear = movies.countByKey.foreach(println)

    val moviesByYear = movies.groupByKey.collect.foreach(println)
  }

  def repartitioning(sc: SparkContext): Unit = {
    println(sc.defaultParallelism)

    val movies = sc.parallelize(List(
      ("Alice in Wonderland", 2016),
      ("Alice Through the Looking Glass", 2010)
    ))
    println(movies.partitions.size)

    println(movies.repartition(2 * sc.defaultParallelism).partitions.size)

    println(movies.coalesce(1).partitions.size)


    //for sorting use range partitioner
    val movies2 = sc.parallelize(List(
      ("Alice in Wonderland", 2016),
      ("Alice Through the Looking Glass", 2010)
    ))
      .partitionBy(new org.apache.spark.HashPartitioner(9))
    println(movies2.partitioner)

  }

  def partitions(sc: SparkContext): Unit = {
    val videos = sc.parallelize(List(
      ("Alice in Wonderland", 2016),
      ("Alice Through the Looking Glass", 2010)
    ))
    println(videos.partitions.size)

    val videos2 = sc.cassandraTable("killr_video", "videos")
    println(videos2.partitions.size)
    println(videos2.partitioner)
  }

  def setBasedIntegrity(sc: SparkContext): Unit = {
    val ratings = sc.cassandraTable("killr_video", "ratings_by_video")
      .keyBy(row => row.getUUID("video_id"))

    val movies = sc.cassandraTable("killr_video", "videos")
      .select("video_id")
      .keyBy(row => row.getUUID("video_id"))

    ratings.subtractByKey(movies)
      .collect
      .foreach(println)
  }

  def useSets(sc: SparkContext): Unit = {
    val A = sc.parallelize(Array(("k1", "v1"), ("k2", "v2"), ("k3", "v3"), ("k4", "v4")))
    val B = sc.parallelize(Array(("k1", "w1"), ("k2", "w2"), ("k3", "w3"), ("k4", "w4")))

    A.union(B)
      .collect
      .foreach(println)

    A.groupByKey
      .join(B.groupByKey)
      .flatMapValues { case (aList, bList) => aList ++ bList }
      .collect
      .foreach(println)
  }

  def referentialIntegrity(sc: SparkContext): Unit = {
    // Also see Joins: join, leftOuterJoin ...
    val playlists = sc.cassandraTable("killr_video", "playlists_by_user")
      .keyBy(row => row.getUUID("movie_id"))

    val movies = sc.cassandraTable("killr_video", "movies")
      .select("movie_id")
      .keyBy(row => row.getUUID("movie_id"))

    playlists.leftOuterJoin(movies)
      .filter { case (m, (rowP, rowM)) => !rowM.isDefined }
      .map { case (m, (rowP, rowM)) => rowP }
      .collect
      .foreach(println)

  }

  def schemaEvolution(sc: SparkContext): Unit = {

    val playlists = sc.cassandraTable("killr_video", "playlists_by_user")
      .select("user_id", "playlist_name", "release_year", "title", "movie_id")
      .as((u: java.util.UUID, p: String, y: Int, t: String, m: java.util.UUID) =>
        (m, (u, p, y, t)))

    val movies = sc.cassandraTable("killr_video", "movies")
      .select("movie_id", "genres", "rating")
      .as((m: java.util.UUID, g: Set[String], r: Option[Float]) =>
        (m, (g, r)))

    playlists.join(movies)
      .map { case (m, ((u, p, y, t), (g, r))) => (u, p, y, t, m, g, r) }
      .saveToCassandra("killr_video", "playlists_by_user")

  }

  def usePairCombineByKey(sc: SparkContext): Unit = {
    // won't work on current docker schema
    sc.cassandraTable[(Int, Option[Float])]("killr_video", "movies_by_actor")
      .where("actor = 'Brad Pitt'")
      .select("release_year", "rating")
      .filter(_._2.isDefined).mapValues(r => r.get)
      .combineByKey(
        (rating: Float) => (rating, 1),
        (res: (Float, Int), rating: Float) => (res._1 + rating, res._2 + 1),
        (res1: (Float, Int), res2: (Float, Int)) => (res1._1 + res2._1, res1._2 + res2._2)
      )
      .mapValues { case (sum, count) => val avg = sum / count; f"$avg%1.1f" }
      .collect
      .foreach(println)
  }

  def usePairFoldByKey(sc: SparkContext): Unit = {
    // won't work on current docker schema
    sc.cassandraTable("killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'")
      .select("release_year", "title", "rating")
      .as((y: Int, t: String, r: Option[Float]) => (y, (t, r)))
      .filter { case (y, (t, r)) => r.isDefined }
      .mapValues { case (t, r) => (t, r.get) }
      .foldByKey(("", 0.0f)) { case ((maxT, maxR), (t, r)) =>
        if (maxR < r) (t, r)
        else (maxT, maxR)
      }
  }

  def usePairReduceByKey(sc: SparkContext): Unit = {
    sc.cassandraTable("killr_video", "videos_by_actor")
      .where("actor = 'Brad Pitt'")
      .select("added_date")
      .as((year: String) => (year, 1))
      .reduceByKey(_ + _)
      .collect
      .foreach(println)
  }

  def usePairRDDs(sc: SparkContext): Unit = {

    sc.cassandraTable[(String, Option[String])]("killr_video", "videos")
      .select("type", "title")
      .mapValues(v => v.getOrElse(0.0))
      .lookup("Movie")
      .foreach(println)

  }

  def cacheRDDs(sc: SparkContext): Unit = {
    // This won't run on present the docker schema
    val movies = sc.cassandraTable("killr_video", "videos")
      .select("release_date", "genres")
      .cache

    val movies2014 = movies.filter(row => row.getInt("release_date") == 2014)
      .cache

    val total2014 = movies2014.count

    val comedy2014 = movies2014.filter(row => row.getSet[String]("genres")
      contains "Comedy").count
    val percentage2014 = 100.0 * comedy2014 / total2014

    val movies2013 = movies.filter(row => row.getInt("release_date") == 2013)
      .cache

    val total2013 = movies2013.count

    val comedy2013 = movies2013.filter(row => row.getSet[String]("genres")
      contains "Comedy").count
    val percentage2013 = 100.0 * comedy2013 / total2013

  }

  def useAccumulatorVariables(sc: SparkContext): Unit = {
    val numRatings = sc.accumulator(0)
    val sumRatings = sc.accumulator(0.0)

    sc.cassandraTable("killr_video", "videos")
      .select("avg_rating")
      .filter(row => row.getStringOption("avg_rating").isDefined) //doesn't filter empty strings
      .foreach { row => numRatings += 1; sumRatings += row.getString("avg_rating").toFloat }

    val avgRating = sumRatings.value / numRatings.value

    println(f"$avgRating%1.1f")
  }

  def useBroadcastVariables(sc: SparkContext): Unit = {
    val popularTitles = sc.broadcast(Set(
      "Alice in Wonderland", "Alice Through the Looking Glass", "..."))

    val movies = sc.cassandraTable("killr_video", "videos")
      .select("title", "release_date", "mpaa_rating", "genres")
      .cache //persist result set in memory only, contrast .persist()

    movies.filter(row => popularTitles.value contains row.getString("title"))
      .saveToCassandra("killr_video", "favorite_movies",
        SomeColumns("title", "release_date", "mpaa_rating", "genres"))

    movies.filter(row => !(popularTitles.value contains row.getString("title")))
      .saveToCassandra("killr_video", "other_movies",
        SomeColumns("title", "release_date", "mpaa_rating", "genres"))
  }

  def saveGenres(sc: SparkContext): Unit = {
    val genres = sc.parallelize(Seq(
      new GenresInfo("Alice in Wonderland", "2010", Set("Adventure", "Family"))
    ))

    genres.saveToCassandra("killrvideo", "favorite_movies",
      SomeColumns("title", "added_date", "genres"))

    val details = sc.parallelize(Seq(
      ("Alive in Wonderland", "2010",
        UDTValue.fromMap(Map("country" -> "USA", "language" -> "English", "runtime" -> 108)))
    ))

    details.saveToCassandra("killrvideo", "favorite_movies",
      SomeColumns("title", "added_date", "details"))

  }

  def saveFavorites(sc: SparkContext): Unit = {
    val movies = sc.cassandraTable("killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'")
      .select("title", "added_date", "description")

    movies.saveToCassandra("killrvideo", "favorite_movies",
      SomeColumns("title", "added_date", "description"))

  }

  def rowsToObjects(sc: SparkContext): Unit = {
    sc.cassandraTable[Record]("killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'")
      .select("added_date", "title", "description")
      .collect
      .foreach(println)

    sc.cassandraTable("killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'")
      .select("added_date", "title", "description")
      .as((y: String, t: String, d: Option[String]) => new Record(y, t, d))
      .collect
      .foreach(println)
  }

  def rowsToTuples(sc: SparkContext): Unit = {
    sc.cassandraTable[(String, String, Option[String])](
      "killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'").select("added_date", "title", "description")
      .collect
      .foreach(println)

    sc.cassandraTable("killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'").select("added_date", "title", "description")
      .as((y: String, t: String, d: Option[String]) => (y, t, d))
      .collect
      .foreach(println)
  }

  def filterVideosByActor(sc: SparkContext): Unit = {
    val movies = sc.cassandraTable("killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'")

    movies.filter(row => row.getString("title").toLowerCase.contains("river"))
      .map { row => row.getString("title") +
        " (" + row.getString("added_date") + ")" +
        " [" + row.getStringOption("description").getOrElse("No description") + "]"
      }
      .collect
      .foreach(println)
  }

  def videosByActor(sc: SparkContext): Unit = {
    sc.cassandraTable("killrvideo", "videos_by_actor")
      .select("title", "added_date")
      .where("actor = 'Brad Pitt'")
      .withDescOrder
      .limit(5)
      .collect
      .foreach(println)
  }

  def filterMovies(sc: SparkContext): Unit = {
    val movies = sc.parallelize(
      Array("Frozen, 2013", "Toy Story, 1995", "WALL-E, 2008",
        "Despicable Me, 2010", "Shrek, 2001",
        "The Lego Movie, 2014", "Alice in Wonderland, 2010")
    )

    val movies2010 = movies.filter(m => m.split(",").last.trim.toInt == 2010)
    movies2010.collect().foreach(println)

    val familyMovies = movies2010.map(m => (m, Set("Family", "Animation")))
    familyMovies.collect().foreach(println)

    val familyGenres = familyMovies.flatMap { case (m, g) => g }
      .distinct
    familyGenres.collect().foreach(println)

    val pairs = familyGenres.cartesian(familyGenres)
      .filter { case (g1, g2) => g1 != g2 }
    pairs.collect().foreach(println)

    val totalLength1 = movies.map(m => m.substring(0, m.length - 6).length)
      .reduce { case (x, y) => x + y }
    println(totalLength1)

    val totalCount = sc.accumulator(0)
    val totalLength = sc.accumulator(0)

    movies.map(m => m.substring(0, m.length - 6).length)
      .foreach { l => totalCount += 1; totalLength += l }
    println(totalLength.value / totalCount.value)
    println(totalLength.value.toDouble / totalCount.value)

  }

  def countWords(sc: SparkContext): Unit = {
    val records = sc.textFile("file:///home/videos.csv")
    records.flatMap(record => record.split(",").drop(1))
      .map(word => (word, 1)).reduceByKey { case (x, y) => x + y }
      .collect().foreach(println)
  }

  def printTest(sc: SparkContext): Unit = {
    val rdd = sc.cassandraTable("test", "kv")
    println(rdd.count)
    println(rdd.first)
    println(rdd.map(_.getInt("value")).sum)
  }

}
