// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO2

package aero_network.aero_network



@SerialVersionUID(0L)
final case class SoftwareTimingPacket(
    timeBucket: scala.Option[Long] = None,
    experimentId: scala.Option[String] = None,
    clientId: scala.Option[String] = None,
    packetId: scala.Option[Long] = None,
    timeSent: scala.Option[Long] = None,
    responseTime: scala.Option[Long] = None
    ) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[SoftwareTimingPacket] with com.trueaccord.lenses.Updatable[SoftwareTimingPacket] {
    @transient
    private[this] var __serializedSizeCachedValue: Int = 0
    private[this] def __computeSerializedValue(): Int = {
      var __size = 0
      if (timeBucket.isDefined) { __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(1, timeBucket.get) }
      if (experimentId.isDefined) { __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, experimentId.get) }
      if (clientId.isDefined) { __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, clientId.get) }
      if (packetId.isDefined) { __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(4, packetId.get) }
      if (timeSent.isDefined) { __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(5, timeSent.get) }
      if (responseTime.isDefined) { __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(6, responseTime.get) }
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
      timeBucket.foreach { __v =>
        _output__.writeInt64(1, __v)
      };
      experimentId.foreach { __v =>
        _output__.writeString(2, __v)
      };
      clientId.foreach { __v =>
        _output__.writeString(3, __v)
      };
      packetId.foreach { __v =>
        _output__.writeInt64(4, __v)
      };
      timeSent.foreach { __v =>
        _output__.writeInt64(5, __v)
      };
      responseTime.foreach { __v =>
        _output__.writeInt64(6, __v)
      };
    }
    def mergeFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): aero_network.aero_network.SoftwareTimingPacket = {
      var __timeBucket = this.timeBucket
      var __experimentId = this.experimentId
      var __clientId = this.clientId
      var __packetId = this.packetId
      var __timeSent = this.timeSent
      var __responseTime = this.responseTime
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 8 =>
            __timeBucket = Some(_input__.readInt64())
          case 18 =>
            __experimentId = Some(_input__.readString())
          case 26 =>
            __clientId = Some(_input__.readString())
          case 32 =>
            __packetId = Some(_input__.readInt64())
          case 40 =>
            __timeSent = Some(_input__.readInt64())
          case 48 =>
            __responseTime = Some(_input__.readInt64())
          case tag => _input__.skipField(tag)
        }
      }
      aero_network.aero_network.SoftwareTimingPacket(
          timeBucket = __timeBucket,
          experimentId = __experimentId,
          clientId = __clientId,
          packetId = __packetId,
          timeSent = __timeSent,
          responseTime = __responseTime
      )
    }
    def getTimeBucket: Long = timeBucket.getOrElse(0L)
    def clearTimeBucket: SoftwareTimingPacket = copy(timeBucket = None)
    def withTimeBucket(__v: Long): SoftwareTimingPacket = copy(timeBucket = Some(__v))
    def getExperimentId: String = experimentId.getOrElse("")
    def clearExperimentId: SoftwareTimingPacket = copy(experimentId = None)
    def withExperimentId(__v: String): SoftwareTimingPacket = copy(experimentId = Some(__v))
    def getClientId: String = clientId.getOrElse("")
    def clearClientId: SoftwareTimingPacket = copy(clientId = None)
    def withClientId(__v: String): SoftwareTimingPacket = copy(clientId = Some(__v))
    def getPacketId: Long = packetId.getOrElse(0L)
    def clearPacketId: SoftwareTimingPacket = copy(packetId = None)
    def withPacketId(__v: Long): SoftwareTimingPacket = copy(packetId = Some(__v))
    def getTimeSent: Long = timeSent.getOrElse(0L)
    def clearTimeSent: SoftwareTimingPacket = copy(timeSent = None)
    def withTimeSent(__v: Long): SoftwareTimingPacket = copy(timeSent = Some(__v))
    def getResponseTime: Long = responseTime.getOrElse(0L)
    def clearResponseTime: SoftwareTimingPacket = copy(responseTime = None)
    def withResponseTime(__v: Long): SoftwareTimingPacket = copy(responseTime = Some(__v))
    def getField(__field: _root_.com.google.protobuf.Descriptors.FieldDescriptor): scala.Any = {
      __field.getNumber match {
        case 1 => timeBucket.orNull
        case 2 => experimentId.orNull
        case 3 => clientId.orNull
        case 4 => packetId.orNull
        case 5 => timeSent.orNull
        case 6 => responseTime.orNull
      }
    }
    override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
    def companion = aero_network.aero_network.SoftwareTimingPacket
}

object SoftwareTimingPacket extends com.trueaccord.scalapb.GeneratedMessageCompanion[aero_network.aero_network.SoftwareTimingPacket] {
  implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[aero_network.aero_network.SoftwareTimingPacket] = this
  def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): aero_network.aero_network.SoftwareTimingPacket = {
    require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
    val __fields = javaDescriptor.getFields
    aero_network.aero_network.SoftwareTimingPacket(
      __fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[Long]],
      __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[String]],
      __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]],
      __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[Long]],
      __fieldsMap.get(__fields.get(4)).asInstanceOf[scala.Option[Long]],
      __fieldsMap.get(__fields.get(5)).asInstanceOf[scala.Option[Long]]
    )
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = AeroNetworkProto.javaDescriptor.getMessageTypes.get(1)
  def messageCompanionForField(__field: _root_.com.google.protobuf.Descriptors.FieldDescriptor): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__field)
  def enumCompanionForField(__field: _root_.com.google.protobuf.Descriptors.FieldDescriptor): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__field)
  lazy val defaultInstance = aero_network.aero_network.SoftwareTimingPacket(
  )
  implicit class SoftwareTimingPacketLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, aero_network.aero_network.SoftwareTimingPacket]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, aero_network.aero_network.SoftwareTimingPacket](_l) {
    def timeBucket: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getTimeBucket)((c_, f_) => c_.copy(timeBucket = Some(f_)))
    def optionalTimeBucket: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.timeBucket)((c_, f_) => c_.copy(timeBucket = f_))
    def experimentId: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getExperimentId)((c_, f_) => c_.copy(experimentId = Some(f_)))
    def optionalExperimentId: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.experimentId)((c_, f_) => c_.copy(experimentId = f_))
    def clientId: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getClientId)((c_, f_) => c_.copy(clientId = Some(f_)))
    def optionalClientId: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.clientId)((c_, f_) => c_.copy(clientId = f_))
    def packetId: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getPacketId)((c_, f_) => c_.copy(packetId = Some(f_)))
    def optionalPacketId: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.packetId)((c_, f_) => c_.copy(packetId = f_))
    def timeSent: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getTimeSent)((c_, f_) => c_.copy(timeSent = Some(f_)))
    def optionalTimeSent: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.timeSent)((c_, f_) => c_.copy(timeSent = f_))
    def responseTime: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getResponseTime)((c_, f_) => c_.copy(responseTime = Some(f_)))
    def optionalResponseTime: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.responseTime)((c_, f_) => c_.copy(responseTime = f_))
  }
  final val TIME_BUCKET_FIELD_NUMBER = 1
  final val EXPERIMENT_ID_FIELD_NUMBER = 2
  final val CLIENT_ID_FIELD_NUMBER = 3
  final val PACKET_ID_FIELD_NUMBER = 4
  final val TIME_SENT_FIELD_NUMBER = 5
  final val RESPONSE_TIME_FIELD_NUMBER = 6
}
