package me.enkode.zk_akka

import concurrent._, duration._
import akka.actor._
import akka.event.LoggingReceive
import org.apache.zookeeper.{WatchedEvent, Watcher, ZooKeeper}
import org.apache.zookeeper.AsyncCallback.DataCallback
import org.apache.zookeeper.data.Stat
import org.apache.zookeeper.Watcher.Event.EventType

object ZkConfigActor {
  // PROTOCOL
  case class Subscribe(subscriber: ActorRef, path: String, andChildren: Boolean)
  case class SubscribeAck(subscribe: Subscribe)
  case class Fetch(path: String, andChildren: Boolean)

  // INTERNAL STATE
  case class ZkConfigState(subscriptions: Map[String, Seq[ActorRef]] = Map.empty) {
    def withSubscription(actorRef: ActorRef, path: String) = copy(subscriptions = {
      subscriptions + {
        path → (subscriptions.getOrElse(path, Nil) :+ actorRef)
      }
    })

    def withoutSubscriber(actorRef: ActorRef) = copy(subscriptions = {
      subscriptions map {
        case (path, subscribers) ⇒ path → (subscribers filterNot { _ == actorRef })
      } filter {
        case (_, subscribers) ⇒ !subscribers.isEmpty
      }
    })
  }

  def props = Props(classOf[ZkConfigActor])
}

class ZkConfigActor extends Actor with ActorLogging with Watcher with ZookeeperOps {
  import ZkConfigActor._
  import ZkConfigExtension._
  import akka.pattern.{ask, pipe}

  val zkTimeout: FiniteDuration = 60.seconds
  implicit val zk = new ZooKeeper("localhost:2181", zkTimeout.toMillis.toInt, this)

  def running(state: ZkConfigState): Receive = LoggingReceive {
    case subscribe@Subscribe(subscriber, path, andChildren) ⇒
      log.debug(s"$sender subscribing to $path")
      sender ! SubscribeAck(subscribe)
      self ! Fetch(path, andChildren)
      context watch subscriber
      context become running(state.withSubscription(subscriber, path))

    case Fetch(path, andChildren) ⇒
      import context.dispatcher
      getData(path, watch = true) map { dataResult ⇒
        ConfigValue(dataResult.path, Option(dataResult.data))
      } pipeTo self

      if (andChildren) {
        getChildren(path, watch = true) map { children ⇒
          children map { child ⇒ self ! Fetch(child, andChildren) }
        }
      }

    case configValue: ConfigValue ⇒
      (state.subscriptions collect {
        case (path, subscribers) if configValue.path startsWith path ⇒ subscribers
      }).flatten map { subscriber ⇒
        subscriber ! configValue
      }

    case Terminated(actorRef) ⇒
      context become running(state.withoutSubscriber(actorRef))
  }

  def receive = running(ZkConfigState())

  def process(event: WatchedEvent) {
    import EventType._
    event.getType match {
      case NodeDataChanged     if event.getPath != null ⇒ self ! Fetch(event.getPath, andChildren = false)
      case NodeChildrenChanged if event.getPath != null ⇒ self ! Fetch(event.getPath, andChildren = true)
      case t ⇒ log.debug(s"unhandled event type $t, ${event.getPath}")
    }
  }
}
