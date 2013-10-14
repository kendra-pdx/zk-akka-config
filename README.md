zk-akka-config
==============

ZooKeeper Configured Akka Actors

```scala
package me.enkode.zk_akka.example

import akka.actor.{Props, ActorSystem, ActorLogging, Actor}
import me.enkode.zk_akka.ZkConfigExtension
import akka.event.LoggingReceive

object ZkConfigExample extends App {
  class ZkConfigExampleActor extends Actor with ActorLogging {
    import ZkConfigExtension._

    override def preStart() = {
      ZkConfigExtension(context.system).subscribe("/test")
    }

    def running(): Receive = LoggingReceive {
      case Subscribed(_, path) ⇒
        log.debug(s"subscribed to $path")

      case ConfigValue(path, data) ⇒
        log.debug(s"$path: ${new String(data)}")
    }

    def receive = running()
  }

  implicit val actorSystem = ActorSystem("ZkConfigExample")
  val actorRef = actorSystem actorOf Props[ZkConfigExampleActor]
}
```
