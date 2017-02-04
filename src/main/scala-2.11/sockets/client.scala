package sockets

import bms_voltage.bms_voltage._
import java.net._
import java.io._

import scala.io._

object client {
  def raw_socket(Arg: Array[String]): Unit = {
    try {
      val s = new Socket("192.168.1.72", 9999)
      val out = new ObjectOutputStream(
        new DataOutputStream(s.getOutputStream)
      )
      val in = new DataInputStream(s.getInputStream)

      out.flush()

      while (true) {
        val buf = Array.fill[Byte](256)(0)
        in.read(buf)
        val message_length = BmsVoltage.defaultInstance.serializedSize
        val message_end = buf.indexOf(0)
        val message = buf.slice(0, message_end)
        val voltageProto = BmsVoltage.parseFrom(message)
        println(voltageProto)
      }
      out.close()
      in.close()
      s.close()
    } catch {
      case e: IOException =>
        e.printStackTrace()
    }
  }

}
