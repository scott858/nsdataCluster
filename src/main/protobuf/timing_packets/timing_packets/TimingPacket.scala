// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO2

package timing_packets.timing_packets



@SerialVersionUID(0L)
final case class TimingPacket(
    clientId: scala.Option[String] = None,
    packetId: scala.Option[Long] = None,
    timeSent: scala.Option[Long] = None,
    responseTime: scala.Option[Long] = None
    ) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[TimingPacket] with com.trueaccord.lenses.Updatable[TimingPacket] {
    @transient
    private[this] var __serializedSizeCachedValue: Int = 0
    private[this] def __computeSerializedValue(): Int = {
      var __size = 0
      if (clientId.isDefined) { __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, clientId.get) }
      if (packetId.isDefined) { __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(2, packetId.get) }
      if (timeSent.isDefined) { __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(3, timeSent.get) }
      if (responseTime.isDefined) { __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(4, responseTime.get) }
      __size
    }
    final override def serializedSize: Int = {
      var read = __serializedSizeCachedValue
      if (read == 0) {
        read = __computeSerializedValue()
        __serializedSizeCachedValue = read
      }
      read
    }
    def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): Unit = {
      clientId.foreach { __v =>
        _output__.writeString(1, __v)
      };
      packetId.foreach { __v =>
        _output__.writeInt64(2, __v)
      };
      timeSent.foreach { __v =>
        _output__.writeInt64(3, __v)
      };
      responseTime.foreach { __v =>
        _output__.writeInt64(4, __v)
      };
    }
    def mergeFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): timing_packets.timing_packets.TimingPacket = {
      var __clientId = this.clientId
      var __packetId = this.packetId
      var __timeSent = this.timeSent
      var __responseTime = this.responseTime
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 10 =>
            __clientId = Some(_input__.readString())
          case 16 =>
            __packetId = Some(_input__.readInt64())
          case 24 =>
            __timeSent = Some(_input__.readInt64())
          case 32 =>
            __responseTime = Some(_input__.readInt64())
          case tag => _input__.skipField(tag)
        }
      }
      timing_packets.timing_packets.TimingPacket(
          clientId = __clientId,
          packetId = __packetId,
          timeSent = __timeSent,
          responseTime = __responseTime
      )
    }
    def getClientId: String = clientId.getOrElse("")
    def clearClientId: TimingPacket = copy(clientId = None)
    def withClientId(__v: String): TimingPacket = copy(clientId = Some(__v))
    def getPacketId: Long = packetId.getOrElse(0L)
    def clearPacketId: TimingPacket = copy(packetId = None)
    def withPacketId(__v: Long): TimingPacket = copy(packetId = Some(__v))
    def getTimeSent: Long = timeSent.getOrElse(0L)
    def clearTimeSent: TimingPacket = copy(timeSent = None)
    def withTimeSent(__v: Long): TimingPacket = copy(timeSent = Some(__v))
    def getResponseTime: Long = responseTime.getOrElse(0L)
    def clearResponseTime: TimingPacket = copy(responseTime = None)
    def withResponseTime(__v: Long): TimingPacket = copy(responseTime = Some(__v))
    def getField(__field: _root_.com.google.protobuf.Descriptors.FieldDescriptor): scala.Any = {
      __field.getNumber match {
        case 1 => clientId.orNull
        case 2 => packetId.orNull
        case 3 => timeSent.orNull
        case 4 => responseTime.orNull
      }
    }
    override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
    def companion = timing_packets.timing_packets.TimingPacket
}

object TimingPacket extends com.trueaccord.scalapb.GeneratedMessageCompanion[timing_packets.timing_packets.TimingPacket] {
  implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[timing_packets.timing_packets.TimingPacket] = this
  def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): timing_packets.timing_packets.TimingPacket = {
    require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
    val __fields = javaDescriptor.getFields
    timing_packets.timing_packets.TimingPacket(
      __fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[String]],
      __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[Long]],
      __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[Long]],
      __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[Long]]
    )
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = TimingPacketsProto.javaDescriptor.getMessageTypes.get(0)
  def messageCompanionForField(__field: _root_.com.google.protobuf.Descriptors.FieldDescriptor): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__field)
  def enumCompanionForField(__field: _root_.com.google.protobuf.Descriptors.FieldDescriptor): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__field)
  lazy val defaultInstance = timing_packets.timing_packets.TimingPacket(
  )
  implicit class TimingPacketLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, timing_packets.timing_packets.TimingPacket]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, timing_packets.timing_packets.TimingPacket](_l) {
    def clientId: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getClientId)((c_, f_) => c_.copy(clientId = Some(f_)))
    def optionalClientId: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.clientId)((c_, f_) => c_.copy(clientId = f_))
    def packetId: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getPacketId)((c_, f_) => c_.copy(packetId = Some(f_)))
    def optionalPacketId: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.packetId)((c_, f_) => c_.copy(packetId = f_))
    def timeSent: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getTimeSent)((c_, f_) => c_.copy(timeSent = Some(f_)))
    def optionalTimeSent: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.timeSent)((c_, f_) => c_.copy(timeSent = f_))
    def responseTime: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getResponseTime)((c_, f_) => c_.copy(responseTime = Some(f_)))
    def optionalResponseTime: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.responseTime)((c_, f_) => c_.copy(responseTime = f_))
  }
  final val CLIENT_ID_FIELD_NUMBER = 1
  final val PACKET_ID_FIELD_NUMBER = 2
  final val TIME_SENT_FIELD_NUMBER = 3
  final val RESPONSE_TIME_FIELD_NUMBER = 4
}
