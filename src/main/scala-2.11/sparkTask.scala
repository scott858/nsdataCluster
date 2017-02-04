/**
  * Created by scott on 6/12/16.
  */

import java.io.{DataInputStream, DataOutputStream, IOException, ObjectOutputStream}
import java.nio.ByteBuffer

import scala.language.implicitConversions
import bms_voltage.bms_voltage._
import org.apache.spark.storage.StorageLevel

import scala.collection.mutable.ArrayBuffer

//import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.streaming._

import akka.util.ByteString
//import akka.zeromq._
import akka.zeromq.Subscribe

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.zeromq._
//import org.apache.spark.streaming.zeromq.ZeroMQUtils

import java.io.{BufferedReader, InputStreamReader}
import java.net.Socket
import java.nio.charset.StandardCharsets

import org.apache.spark.internal.Logging
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.receiver.Receiver


object sparkTask {

  val streamSource = "172.17.0.1"
  val assemblyPath = "/home/scott/repos/code/nsdataCluster/" +
    "target/scala-2.11/nsdataCluster-assembly-1.0.jar"

  val sparkMaster = "172.17.0.2"
  val sparkPort = "7077"

  def receiveTest() {
    try {
      val socket = new Socket("172.17.0.1", 9999)
      val stream = new DataInputStream(socket.getInputStream)

      var msgEnd = 0
      val messageBufferLength = 65535
      val messageBuffer = Array.fill[Byte](messageBufferLength + 1)(1)
      while (true) {
        if (messageBuffer.contains(0)) {
          msgEnd = messageBuffer.indexOf(0)
          val message = messageBuffer.slice(0, msgEnd).map(x => (x & 0xFF).toChar)
          val messageString = new String(message)
          val messageStringArray = ArrayBuffer.fill[String](1)(messageString)
//          val messageBytesArray = messageStringArray.map(x => x.map(y => y))

          for (i <- 0 until messageBufferLength - msgEnd) {
            messageBuffer(i) = messageBuffer(i + 1 + msgEnd)
          }
        } else {
          stream.read(
            messageBuffer,
            msgEnd,
            messageBufferLength - msgEnd
          )
        }
      }

      stream.close()
      socket.close()
    } catch {
      case e: IOException =>
        e.printStackTrace()
      case e: java.net.ConnectException =>
      case t: Throwable =>
    }
  }

  def main(args: Array[String]) = {
    saveBmsVoltageStreamToCassandra()
//        receiveTest()
  }

  def saveBmsVoltageStreamToCassandra(): Unit = {
    val conf = new SparkConf(true)
      .setAppName("Aero BMS")
      .setMaster("spark://" + sparkMaster + ":" + sparkPort)
      .set("spark.executor.memory", "2G")
      .set("spark.cassandra.connection.host", sparkMaster)
      .set("spark.cassandra.output.consistency.level", "ONE")
      .set("spark.cleaner.ttl", "3600")

    val cc = com.datastax.spark.connector.cql.CassandraConnector(conf)
    setupSchema(cc)

    def bytesToStringIterator(x: Seq[ByteString]): Iterator[String] =
      x.map(_.utf8String).iterator

    val ssc = new StreamingContext(conf, Seconds(4))
    //    ssc.sparkContext.setLogLevel("WARN")
    //    val stream = ssc.socketTextStream(streamSource, 9999, StorageLevel.MEMORY_ONLY)
    //    val stream = ssc.rawSocketStream(streamSource, 9999, StorageLevel.MEMORY_ONLY)
    val stream = ssc.receiverStream(new CustomReceiver("172.17.0.1", 9999))

    //    val stream = ZeroMQUtils.createStream(
    //      ssc,
    //      "tcp://172.17.0.1:9999",
    //      Subscribe(""),
    //      bytesToStringIterator _
    //    )

    //    val words = stream.flatMap(_.split(" "))
    //    val wordCounts = words.map(x => (x,1)).reduceByKey(_+_)
    //    wordCounts.print
    //    val protobytes = stream.flatMap(x => x.split(0.toString)).map(x => x.getBytes)
    //    val protobytes = stream.flatMap(x => x.split(0.toString)).map(x => x.getBytes)
    val protobytes = stream.flatMap(x => x.split("\0"))
    val protobuf = protobytes.map(x => parseBmsVoltageProtobuf(x))
    protobuf.saveToCassandra("aerobms", "cell_voltages")

    ssc.start()
    ssc.awaitTermination()
  }

  def parseBmsVoltageProtobuf(packet: String): BmsVoltage = {
    printPacket(packet)

    try {
      BmsVoltage.fromAscii(packet)
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

  case class ParsedBmsVoltagePacket(id: String, d: String, f: String, u: String)

  def parseBmsVoltagePacket(packet: String): ParsedBmsVoltagePacket = {
    packet.split(" ") match {
      case Array(f, u, d, id) => ParsedBmsVoltagePacket(id, f, u, d)
      case _ => ParsedBmsVoltagePacket("0", "0", "0", "0")
    }
  }

  def saveStreamToCassandraTest(): Unit = {
    val conf = new SparkConf(true)
      .setAppName("Streaming Example")
      .setMaster("spark://" + sparkMaster + ":" + sparkPort)
      .set("spark.executor.memory", "2G")
      .set("spark.cassandra.connection.host", sparkMaster)
      .set("spark.cassandra.output.consistency.level", "ONE")
      .set("spark.cleaner.ttl", "3600")

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
    val stream = ssc.socketTextStream(streamSource, 9999)

    stream.map(record => record.split("\n"))
      .map { case Array(p) => parseBmsVoltagePacket(p) }
      .saveToCassandra("test_data", "fuck_you")

    ssc.start()
    ssc.awaitTermination()
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
  //  private def receive() {
  //    var socket: Socket = null
  //    var messageByte: Int = 1
  //    val messageBuffer = Array.fill[Int](1024)(0)
  //
  //    try {
  //      socket = new Socket(host, port)
  //      val reader = new BufferedReader(
  //        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
  //
  //      messageByte = reader.read()
  //      messageBuffer(0) = messageByte
  //      var count = 1
  //      while (!isStopped & messageByte >= 0) {
  //        while (messageByte > 0 && count < messageBuffer.length) {
  //          messageByte = reader.read()
  //          messageBuffer(count) = messageByte
  //          count += 1
  //        }
  //        if (messageByte == 0) {
  //          val messageArray = ArrayBuffer
  //            .fill[String](1)(messageBuffer.slice(0, count).mkString)
  //
  //          store(messageArray)
  //          count = 0
  //        }
  //        if(count > messageBuffer.length){
  //          count = 0
  //        }
  //      }
  //      reader.close()
  //      socket.close()
  //      restart("Trying to connect again")
  //    } catch {
  //      case e: java.net.ConnectException =>
  //        restart("Error connecting to " + host + ":" + port, e)
  //      case t: Throwable =>
  //        restart("Error receiving data", t)
  //    }
  //  }

  private def receive() {
    try {
      val socket = new Socket("172.17.0.1", 9999)
      val stream = new DataInputStream(socket.getInputStream)

      var msgEnd = 0
      val messageBufferLength = 65535
      val messageBuffer = Array.fill[Byte](messageBufferLength + 1)(1)
      while (true) {
        val messageStringArray = ArrayBuffer.fill[String](1)("")
        var messageCount = 0
        if (messageBuffer.contains(0)) {
          msgEnd = messageBuffer.indexOf(0)

          val message = messageBuffer.slice(0 + 1, msgEnd + 2).map(x => (x & 0xFF).toChar)
          message(0) = 0
          val messageString = new String(message)
          messageStringArray(messageCount) = messageString

          for (i <- 0 until messageBufferLength - msgEnd) {
            messageBuffer(i) = messageBuffer(i + 1 + msgEnd)
          }
          messageCount += 1
        } else {
          stream.read(
            messageBuffer,
            msgEnd,
            messageBufferLength - msgEnd
          )
        }
        store(messageStringArray)
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

  private def receive2() {
    var socket: Socket = null
    var userInput: String = null
    try {
      socket = new Socket(host, port)
      val reader = new BufferedReader(
        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
      userInput = reader.readLine()
      while (!isStopped && userInput != null) {
        store(userInput)
        userInput = reader.readLine()
      }
      reader.close()
      socket.close()
      restart("Trying to connect again")
    } catch {
      case e: java.net.ConnectException =>
        restart("Error connecting to " + host + ":" + port, e)
      case t: Throwable =>
        restart("Error receiving data", t)
    }
  }
}
