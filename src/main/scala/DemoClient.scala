import akka.actor.{Actor, ActorLogging, ActorPath, ActorSystem, Props}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import com.typesafe.config.ConfigFactory

object DemoClient {

  def main(args : Array[String]) {
    val config = ConfigFactory.parseString("""
     akka {
       log-dead-letters = OFF
       actor {
         provider = "akka.remote.RemoteActorRefProvider"
       }

       remote {
         transport = "akka.remote.netty.NettyRemoteTransport"
         log-remote-lifecycle-events = off
         netty.tcp {
          hostname = "localhost"
          port = 5000
         }
       }
     }""")

    val system = ActorSystem("OUTSIDER-SYSTEM", ConfigFactory.load(config))

    val initialContacts = Set(
      ActorPath.fromString("akka.tcp://ClusterSystem@localhost:2551/system/receptionist"))


    val cc = system.actorOf(ClusterClient.props(
      ClusterClientSettings(system).withInitialContacts(initialContacts)), "os-client")

    val ccActor = system.actorOf(Props[ClusterClientActor], "ccActor")

    cc ! ClusterClient.Send("/user/master", ccActor, localAffinity = true)

    (1 to 10).map { i =>
      cc ! ClusterClient.Send("/user/master", s"hello - $i", localAffinity = true)
      Thread.sleep(10000)
    }
  }

  class ClusterClientActor extends Actor with ActorLogging {
    def receive: Receive = {
      case e =>
        log.info(s"from cluster-client : $e : $sender")
    }
  }
}
