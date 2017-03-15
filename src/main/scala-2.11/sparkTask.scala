import java.io._
import java.util.UUID
import java.nio.ByteBuffer

//import com.github.nscala_time.time.Imports._

import scala.language.implicitConversions
import bms_voltage.bms_voltage._
import aero_network.aero_network._

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

  val assemblyPath = "/home/scott/repos/code/nsdataCluster/" +
    "target/scala-2.11/nsdataCluster-assembly-1.0.jar"

  //  val streamSource = "10.10.10.12"
  //  val sparkMaster = "10.10.10.15"
  val streamSource = "192.168.1.72"
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

    saveNetworkPtp1588TimingPacketToCassandra(ssc, cc, stream)
  }

  def saveNetworkSoftwareTimingPacketToCassandra(ssc: StreamingContext,
                                                 cc: CassandraConnector,
                                                 stream: ReceiverInputDStream[String]): Unit = {
    setupNetworkSoftwareTimingSchema(cc)
    stream.map(x => x.map(y => y.toByte).to[Array])
      .map(x => parseSoftwareNetworkPacketProtobuf(x))
      .map(x => ConvertSoftwareTimingPacket(x))
      .saveToCassandra("aeronetwork", "software_timing_packet")

    ssc.start()
    ssc.awaitTermination()
  }

  def setupNetworkSoftwareTimingSchema(cc: CassandraConnector): Unit = {
    cc.withSessionDo { session =>
      session.execute("create keyspace if not exists " +
        "aeronetwork " +
        "with " +
        "replication = {'class': 'SimpleStrategy', 'replication_factor': 1}")
    }

    cc.withSessionDo(session => session.execute("use aeronetwork"))

    //    cc.withSessionDo(session =>
    //      session.execute("drop table if exists " +
    //        "experiments")
    //    )
    //
    //    cc.withSessionDo(session =>
    //      session.execute("drop table if exists " +
    //        "timing_packet")
    //    )

    cc.withSessionDo { session =>
      session.execute("create table if not exists " +
        "aeronetwork.experiments( " +
        "experiment_id uuid, " +
        "time timestamp, " +
        "description text, " +
        "primary key((experiment_id, time)));")
    }

    cc.withSessionDo { session =>
      session.execute("create table if not exists " +
        "aeronetwork.software_timing_packet( " +
        "time_bucket timestamp, " +
        "experiment_id uuid, " +
        "client_id text, " +
        "packet_id bigint, " +
        "time_sent bigint, " +
        "response_time int, " +
        "primary key((experiment_id, client_id, time_bucket), packet_id, time_sent));")
    }

  }

  case class UuidSoftwareTimingPacket(timeBucket: Long, experimentId: UUID, clientId: String,
                                      packetId: Long, timeSent: Long, responseTime: Long)

  def ConvertSoftwareTimingPacket(packet: SoftwareTimingPacket): UuidSoftwareTimingPacket = {
    UuidSoftwareTimingPacket(
      timeBucket = packet.timeBucket.get,
      experimentId = UUID.fromString(packet.experimentId.get),
      clientId = packet.clientId.get,
      packetId = packet.packetId.get,
      timeSent = packet.timeSent.get,
      responseTime = packet.responseTime.get
    )
  }

  def parseSoftwareNetworkPacketProtobuf(packet: Array[Byte]): SoftwareTimingPacket = {
    try {
      if (packet.length > 0) {
        SoftwareTimingPacket.parseFrom(packet)
      } else {
        ZeroSoftwareTimingPacket()
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        ErrorSoftwareTimingPacket()
    }
  }

  def ZeroSoftwareTimingPacket(): SoftwareTimingPacket = {
    SoftwareTimingPacket()
  }

  def ErrorSoftwareTimingPacket(): SoftwareTimingPacket = {
    SoftwareTimingPacket(
      clientId = Option("error"),
      packetId = Option(1)
    )
  }

  def setupNetworkPTP1588TimingSchema(cc: CassandraConnector): Unit = {
    cc.withSessionDo { session =>
      session.execute("create keyspace if not exists " +
        "aeronetwork " +
        "with " +
        "replication = {'class': 'SimpleStrategy', 'replication_factor': 1}")
    }

    cc.withSessionDo(session => session.execute("use aeronetwork"))

    //    cc.withSessionDo(session =>
    //      session.execute("drop table if exists " +
    //        "experiments")
    //    )
    //
    //    cc.withSessionDo(session =>
    //      session.execute("drop table if exists " +
    //        "timing_packet")
    //    )

    cc.withSessionDo { session =>
      session.execute("create table if not exists " +
        "aeronetwork.experiments( " +
        "experiment_id uuid, " +
        "time timestamp, " +
        "description text, " +
        "primary key((experiment_id, time)));")
    }

    cc.withSessionDo { session =>
      session.execute("create table if not exists " +
        "aeronetwork.ptp1588_timing_packet( " +
        "time_bucket timestamp, " +
        "experiment_id uuid, " +
        "mac_address bigint, " +
        "sync_receive_s int, " +
        "sync_receive_ns int, " +
        "delay_request_send_s int, " +
        "delay_request_send_ns int, " +
        "delay_request_receive_s int, " +
        "delay_request_receive_ns int, " +
        "offset_from_master_s int, " +
        "offset_from_master_ns int, " +
        "mean_path_delay_s int, " +
        "mean_path_delay_ns int, " +
        "master_to_slave_delay_s int, " +
        "master_to_slave_delay_ns int, " +
        "slave_to_master_delay_s int, " +
        "slave_to_master_delay_ns int, " +
        "port_state int, " +
        "primary key((experiment_id, mac_address, time_bucket), delay_request_receive_s, delay_request_receive_ns));")
    }
  }

  def saveNetworkPtp1588TimingPacketToCassandra(ssc: StreamingContext,
                                                cc: CassandraConnector,
                                                stream: ReceiverInputDStream[String]): Unit = {
    setupNetworkPTP1588TimingSchema(cc)
    stream.map(x => x.map(y => y.toByte).to[Array])
      .map(x => parsePTP1588NetworkPacketProtobuf(x))
      .map(x => ConvertPTP1588TimingPacket(x))
      .saveToCassandra("aeronetwork", "ptp1588_timing_packet")

    ssc.start()
    ssc.awaitTermination()
  }

  case class UuidPtp1588TimingPacket(timeBucket: Long,
                                     experimentId: UUID,
                                     macAddress: Long,
                                     syncReceiveS: Int,
                                     syncReceiveNs: Int,
                                     delayRequestSendS: Int,
                                     delayRequestSendNs: Int,
                                     delayRequestReceiveS: Int,
                                     delayRequestReceiveNs: Int,
                                     offsetFromMasterS: Int,
                                     offsetFromMasterNS: Int,
                                     meanPathDelayS: Int,
                                     meanPathDelayNs: Int,
                                     masterToSlaveDelayS: Int,
                                     masterToSlaveDelayNs: Int,
                                     slaveToMasterDelayS: Int,
                                     slaveToMasterDelayNs: Int,
                                     portState: Int)

  def ConvertPTP1588TimingPacket(packet: Ptp1588TimingPacket): UuidPtp1588TimingPacket = {
    UuidPtp1588TimingPacket(
      experimentId = UUID.fromString(packet.experimentId.get),
      timeBucket = packet.timeBucket.get,
      macAddress = packet.macAddress.get,
      syncReceiveS = packet.syncReceiveS.get,
      syncReceiveNs = packet.syncReceiveNs.get,
      delayRequestSendS = packet.delayRequestSendS.get,
      delayRequestSendNs = packet.delayRequestSendNs.get,
      delayRequestReceiveS = packet.delayRequestReceiveS.get,
      delayRequestReceiveNs = packet.delayRequestReceiveNs.get,
      offsetFromMasterS = packet.offsetFromMasterS.get,
      offsetFromMasterNS = packet.offsetFromMasterNs.get,
      meanPathDelayS = packet.meanPathDelayS.get,
      meanPathDelayNs = packet.meanPathDelayNs.get,
      masterToSlaveDelayS = packet.masterToSlaveDelayS.get,
      masterToSlaveDelayNs = packet.masterToSlaveDelayNs.get,
      slaveToMasterDelayS = packet.masterToSlaveDelayS.get,
      slaveToMasterDelayNs = packet.masterToSlaveDelayNs.get,
      portState = packet.portState.get
    )
  }

  def parsePTP1588NetworkPacketProtobuf(packet: Array[Byte]): Ptp1588TimingPacket = {
    try {
      if (packet.length > 0) {
        Ptp1588TimingPacket.parseFrom(packet)
      } else {
        sparkTask.ZeroPtp1588TimingPacket()
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        ErrorPtp1588TimingPacket()
    }
  }

  def ZeroPtp1588TimingPacket(): Ptp1588TimingPacket = {
    Ptp1588TimingPacket()
  }

  def ErrorPtp1588TimingPacket(): Ptp1588TimingPacket = {
    Ptp1588TimingPacket(
      macAddress = Option(-1)
    )
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
      socket.connect("tcp://" + sparkTask.streamSource + ":9999")
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
