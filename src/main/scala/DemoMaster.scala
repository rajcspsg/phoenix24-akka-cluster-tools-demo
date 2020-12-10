import akka.actor.{ActorRef, _}
import akka.cluster.client.ClusterClientReceptionist
import com.typesafe.config.ConfigFactory

object DemoMaster {

  def main(args: Array[String]): Unit = {

    val config = ConfigFactory.parseString("""
     akka {
       log-dead-letters = OFF
       extensions = ["akka.cluster.client.ClusterClientReceptionist"]
       actor {
         provider = "akka.cluster.ClusterActorRefProvider"
       }
       remote {
         transport = "akka.remote.netty.NettyRemoteTransport"
         log-remote-lifecycle-events = off
         netty.tcp {
           hostname = "localhost"
           port = 2551
         }
       }
       cluster {
         seed-nodes = [
           "akka.tcp://ClusterSystem@localhost:2551"
           ]
         roles = [master]
         auto-down = on
       }
     }""")

    val system = ActorSystem("ClusterSystem", ConfigFactory.load(config))

    val master = system.actorOf(Props[ClusterMaster], "master")
    ClusterClientReceptionist(system).registerService(master)
  }

  class ClusterMaster extends Actor with ActorLogging {
    var senderActor : ActorRef = null;

    def receive: Receive = {
      case a: ActorRef =>
        log.info(s"from master : $a : $sender")
        senderActor = a
      case e =>
        log.info(s"from master : $e : $senderActor")
        if(senderActor != null) senderActor ! "master : how are you?"
    }
  }
}
