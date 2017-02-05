/**
  * Created by scott on 6/12/16.
  */

import java.io._

import scala.language.implicitConversions
import bms_voltage.bms_voltage._

import scala.collection.mutable.ArrayBuffer

import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.streaming._

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

import java.net.Socket

import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.receiver.Receiver


object sparkTask {

  val streamSource = "192.168.0.4"
  val assemblyPath = "/home/scott/repos/code/nsdataCluster/" +
    "target/scala-2.11/nsdataCluster-assembly-1.0.jar"

  val sparkMaster = streamSource
  val sparkPort = "7077"

  def main(args: Array[String]) = {
    saveBmsVoltageSocketStreamToCassandra()
    //    receiveTest()
  }

  def saveBmsVoltageSocketStreamToCassandra(): Unit = {
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

    val stream = ssc.receiverStream(new CustomReceiver(sparkMaster, 9999))

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
      val socket = new Socket("192.168.0.4", 9999)
      val stream = new DataInputStream(socket.getInputStream)

      val messageBufferLength = 65535
      val messageBuffer = Array.fill[Byte](messageBufferLength + 1)(1)
      var bytesRead = 0
      var messageTailFragment: String = null
      while (true) {
        bytesRead = stream.read(messageBuffer)
        val receivedString = new String(
          messageBuffer.slice(0, bytesRead)
            .map(x => if (x < 0) 256 + x else x).map(_.toChar)
        )
        val messages = receivedString.split("\0")

        if (messages(0) != "") {
          if (messageTailFragment != "") {
            messages(0) = messageTailFragment + messages(0)
          }
        }
        if (messages.last != "") {
          messageTailFragment = messages.last
        }

        store(messages.to[ArrayBuffer])
      }
      stream.close()
      socket.close()
    } catch {
      case e: IOException =>
        e.printStackTrace()
      case e: java.net.ConnectException =>
        restart("Error connecting to " + host + ":" + port, e)
      case t: Throwable =>
        restart("Error receiving data", t)
    }
  }

}
