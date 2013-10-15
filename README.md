zk-akka-config
==============

ZooKeeper Configured Akka Actors

```scala
package me.enkode.zk_akka.example

import concurrent._, duration._
import akka.actor.{Props, ActorSystem, ActorLogging, Actor}
import me.enkode.zk_akka.{DefaultDataUnmarshallers, ZkConfigExtension}

object ZkConfigExample extends App {
  case object Poke

  class ZkConfigExampleActor extends Actor with ActorLogging with DefaultDataUnmarshallers {
    import ZkConfigExtension._

    override def preStart() = {
      ZkConfigExtension(context.system).subscribe("/test")
    }

    def waitingForConfig(): Receive = {
      case Subscribed(_, path) ⇒
        log.debug(s"subscribed to $path")

      case cv@ConfigValue(path, _) ⇒
        cv.dataAs[String] map { config ⇒
          log.debug(s"$path: $config")
          context become (waitingForConfig() orElse configured(config))
        }
    }

    def configured(config: String): Receive = {
      case Poke ⇒
        log.debug(s"doing something with my config, which is: $config")
    }

    def receive = waitingForConfig()
  }

  implicit val actorSystem = ActorSystem("ZkConfigExample")
  import actorSystem.dispatcher
  val actorRef = actorSystem actorOf Props[ZkConfigExampleActor]

  actorSystem.scheduler.schedule(1.second, 5.seconds, actorRef, Poke)
}
```
