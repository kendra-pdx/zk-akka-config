package me.enkode.zk_akka

import me.enkode.zk_akka.ZkConfigExtension.ValueUnmarshaller

trait DefaultDataUnmarshallers {
  implicit object StringValueUnmarshaller extends ValueUnmarshaller[String] {
    def unmarshal(data: Array[Byte]): String = new String(data)
  }
}

object DefaultDataUnmarshallers extends DefaultDataUnmarshallers
