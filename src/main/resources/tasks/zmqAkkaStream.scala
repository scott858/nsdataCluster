package tasks

import akka.util.ByteString
import akka.zeromq.Subscribe
import bms_voltage.bms_voltage._
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.streaming._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.zeromq._
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.language.implicitConversions


object zmqAkkaStream {

  val streamSource = "192.168.0.4"
  val assemblyPath = "/home/scott/repos/code/nsdataCluster/" +
    "target/scala-2.11/nsdataCluster-assembly-1.0.jar"

  val sparkMaster = streamSource
  val sparkPort = "7077"

  def mainAkkaZMQ(args: Array[String]) = {
    saveBmsVoltageZmqStreamToCassandra()
    //    receiveTest()
  }

  def saveBmsVoltageZmqStreamToCassandra(): Unit = {
    val conf = new SparkConf(true)
      .setAppName("Aero BMS")
      .setMaster("spark://" + sparkMaster + ":" + sparkPort)
      .set("spark.executor.memory", "2G")
      .set("spark.cassandra.connection.host", sparkMaster)
      .set("spark.cassandra.output.consistency.level", "ONE")
      .set("spark.cleaner.ttl", "3600")

    val cc = com.datastax.spark.connector.cql.CassandraConnector(conf)
    setupSchema(cc)

    val ssc = new StreamingContext(conf, Seconds(4))
    ssc.sparkContext.setLogLevel("WARN")

    def bytesToStringIterator(x: Seq[ByteString]): Iterator[String] =
      x.map(_.utf8String).iterator

    val stream = ZeroMQUtils.createStream(
      ssc,
      "tcp://192.168.0.4:9999",
      Subscribe(""),
      bytesToStringIterator _
    )

    stream.map(x => x.map(y => y.toByte).to[Array])
      .map(x => parseBmsVoltageProtobuf(x))
      .saveToCassandra("aerobms", "cell_voltages")

    ssc.start()
    ssc.awaitTermination()
  }

  def parseBmsVoltageProtobuf(packet: Array[Byte]): BmsVoltage = {
    try {
      if (packet.length > 0) {
        BmsVoltage.parseFrom(packet)
      } else {
        zeroBmsVoltageBuff()
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        errorBmsVoltageBuff()
    }
  }

  def zeroBmsVoltageBuff(): BmsVoltage = {
    BmsVoltage(
      realTime = 0,
      cpuTime = 0,
      deviceId = 0,
      voltage0 = 0,
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

  def errorBmsVoltageBuff(): BmsVoltage = {
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

  def setupSchema(cc: CassandraConnector): Unit = {
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


  }

  def printPacket(packet: String): Unit = {
    println()
    println(packet.length)
    packet.foreach(x => print(x.toInt + " "))
    println()
  }
}
