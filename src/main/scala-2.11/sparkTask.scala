import java.io._
import java.nio.ByteBuffer

import scala.language.implicitConversions
import bms_voltage.bms_voltage._
import timing_packets.timing_packets._

import scala.collection.mutable.ArrayBuffer
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.streaming._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.receiver.Receiver
import zeromq.{Message, SocketType, ZeroMQ}

import scala.concurrent.Await
import scala.concurrent.duration._


object sparkTask {

  val streamSource = "192.168.0.4"
  val assemblyPath = "/home/scott/repos/code/nsdataCluster/" +
    "target/scala-2.11/nsdataCluster-assembly-1.0.jar"

  val sparkMaster = streamSource
  val sparkPort = "7077"

  def receiveTest() {
    try {

      val socket = ZeroMQ.socket(SocketType.Sub)
      socket.connect("tcp://192.168.1.72:9999")
      socket.subscribe("")

      while (true) {
        val messageOption = socket.recvOption
        messageOption match {
          case Some(message) =>
            for (i <- 0 until message.length) {
              val messageString = new String(
                message(i).map(x => if (x < 0) (256 + x).toChar else x.toChar).toArray
              )
              println(messageString.map(x => x.toInt + " "))
            }
          case _ =>
        }
      }
      socket.close

    } catch {
      case e: IOException =>
        e.printStackTrace()
      case e: java.net.ConnectException =>
      case e: Exception =>
        e.printStackTrace()
      case t: Throwable =>
    }
  }

  def main(args: Array[String]) = {
    val conf = new SparkConf(true)
      .setAppName("Aero BMS")
      .setMaster("spark://" + sparkMaster + ":" + sparkPort)
      .set("spark.executor.memory", "2G")
      .set("spark.cassandra.connection.host", sparkMaster)
      .set("spark.cassandra.output.consistency.level", "ONE")
      .set("spark.cleaner.ttl", "3600")

    val cc = com.datastax.spark.connector.cql.CassandraConnector(conf)

    val ssc = new StreamingContext(conf, Seconds(4))
    ssc.sparkContext.setLogLevel("WARN")

    val stream = ssc.receiverStream(new CustomReceiver(sparkMaster, 9999))

    saveNetworkTimingPacketToCassandra(ssc, cc, stream)
//    saveBmsVoltageZmqStreamToCassandra(ssc, cc, stream)
//    receiveTest()
  }

  def saveNetworkTimingPacketToCassandra(ssc: StreamingContext,
                                         cc: CassandraConnector,
                                         stream: ReceiverInputDStream[String]): Unit = {
    setupNetworkTimingSchema(cc)
    stream.map(x => x.map(y => y.toByte).to[Array])
      .map(x => parseNetworkTimingPacketProtobuf(x))
      .saveToCassandra("aeronetwork", "timing_packet")

    ssc.start()
    ssc.awaitTermination()
  }

  def parseNetworkTimingPacketProtobuf(packet: Array[Byte]): TimingPacket = {
    try {
      if (packet.length > 0) {
        TimingPacket.parseFrom(packet)
      } else {
        zeroTimingPacket()
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        errorTimingPacket()
    }
  }

  def zeroTimingPacket(): TimingPacket = {
    TimingPacket()
  }

  def errorTimingPacket(): TimingPacket = {
    TimingPacket(
      clientId = Option("error"),
      packetId = Option(1)
    )
  }

  def setupNetworkTimingSchema(cc: CassandraConnector): Unit = {
    cc.withSessionDo { session =>
      session.execute("create keyspace if not exists " +
        "aeronetwork " +
        "with " +
        "replication = {'class': 'SimpleStrategy', 'replication_factor': 1}")
    }

    cc.withSessionDo(session => session.execute("use aeronetwork"))

    cc.withSessionDo(session =>
      session.execute("drop table if exists " +
        "timing_packet")
    )

    cc.withSessionDo { session =>
      session.execute("create table if not exists " +
        "aeronetwork.timing_packet ( " +
        "client_id text, " +
        "packet_id int, " +
        "time_sent int, " +
        "response_time int, " +
        "primary key((client_id, packet_id), time_sent));")
    }

  }

  def saveBmsVoltageZmqStreamToCassandra(ssc: StreamingContext,
                                         cc: CassandraConnector,
                                         stream: ReceiverInputDStream[String]): Unit = {
    setupBmsVoltageSchema(cc)
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

  def setupBmsVoltageSchema(cc: CassandraConnector): Unit = {
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

class CustomReceiver(host: String, port: Int)
  extends Receiver[String](StorageLevel.MEMORY_AND_DISK_2) {

  def onStart() {
    // Start the thread that receives data over a connection
    new Thread("Socket Receiver") {
      override def run() {
        receive()
      }
    }.start()
  }

  def onStop() {
    // There is nothing much to do as the thread calling receive()
    // is designed to stop by itself isStopped() returns false
  }

  /** Create a socket connection and receive data until receiver is stopped */
  private def receive() {
    try {
      val socket = ZeroMQ.socket(SocketType.Sub)
      socket.connect("tcp://192.168.0.4:9999")
      socket.subscribe("")

      while (!isStopped()) {
        val messageOption = socket.recvOption
        messageOption match {
          case Some(message) =>
            for (i <- 0 until message.length) {
              val messageString = new String(
                message(i).map(x => if (x < 0) (256 + x).toChar else x.toChar).toArray
              )
              store(messageString)
            }
          case _ =>
        }
      }
      socket.close

    } catch {
      case e: IOException =>
        e.printStackTrace()
      case e: java.net.ConnectException =>
        restart("Error connecting to " + host + ":" + port, e)
      case e: Exception =>
        e.printStackTrace()
      case t: Throwable =>
        restart("Error receiving data", t)
    }
  }
}
