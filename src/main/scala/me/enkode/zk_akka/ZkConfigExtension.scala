package me.enkode.zk_akka

import scala.concurrent._, duration._
import scala.util.control.NonFatal
import scala.util.{Try, Success, Failure}
import akka.actor._
import org.apache.zookeeper.{WatchedEvent, Watcher, ZooKeeper}
import akka.util.Timeout
import org.slf4j.LoggerFactory

object ZkConfigExtension extends ExtensionId[ZkConfigExtension] with ExtensionIdProvider {
  // PROTOCOL
  case class Subscribed(actorRef: ActorRef, path: Try[String])
  case class ConfigValue(path: String, data: Array[Byte])

  def lookup() = ZkConfigExtension
  def createExtension(system: ExtendedActorSystem): ZkConfigExtension = {
    new ZkConfigExtension(system, system.actorOf(ZkConfigActor.props))
  }
}

class ZkConfigExtension(
  actorSystem: ActorSystem,
  zkConfigActor: ActorRef)
  extends Extension {
  import ZkConfigActor._
  import ZkConfigExtension._

  val logger = LoggerFactory.getLogger(classOf[ZkConfigExtension])

  def subscribe(paths: String*)(implicit context: ActorContext): Unit = paths map { path ⇒ subscribe(path) }

  def subscribe(path: String)(implicit context: ActorContext): Unit = {
    import akka.pattern.{ask, pipe}
    implicit val askTimeout = new Timeout(1.seconds)
    import context.dispatcher

    logger.debug(s"${context.self} subscribing to $path")
    (zkConfigActor ? Subscribe(context.self, path)).mapTo[SubscribeAck] map { ack ⇒
      logger.debug(ack.toString)
      Subscribed(context.self, Success(path))
    } recover {
      case NonFatal(t) ⇒
        Subscribed(context.self, Failure(t))
    } pipeTo context.self
  }
}
