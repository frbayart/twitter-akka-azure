package io.kensu.frbayart

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef

import twitter4j._
import twitter4j.conf.ConfigurationBuilder
import twitter4j.FilterQuery
import com.typesafe.config.ConfigFactory
import com.microsoft.azure.eventhubs._

import io.kensu.frbayart.models._


object TwitterStreamer extends App {
    val conf = ConfigFactory.load()
    val twitterConfig = conf.getConfig("twitter")

    implicit val system = akka.actor.ActorSystem()
    import system.dispatcher
    import akka.actor.Props
    val eventhubActor = system.actorOf(Props(new AzureEventHubActor()), "azure-eventhub")
    val streamActor = system.actorOf(Props(new TweetsCollector(eventhubActor)), "twitter-stream")


    val cb = new ConfigurationBuilder()
    cb.setDebugEnabled(true)
      .setOAuthConsumerKey(twitterConfig.getString("consumerKey"))
      .setOAuthConsumerSecret(twitterConfig.getString("consumerSecret"))
      .setOAuthAccessToken(twitterConfig.getString("accessToken"))
      .setOAuthAccessTokenSecret(twitterConfig.getString("accessTokenSecret"))

    val twitterStream = new TwitterStreamFactory(cb.build()).getInstance()
    val listener = new StatusListener() {
        def onStatus(status: Status) {
            streamActor ! status
        }
        def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {}
        def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {}
        def onException(ex:java.lang.Exception) {
            ex.printStackTrace()
        }
        def onScrubGeo(lat: Long, long: Long){}
        def onStallWarning(s: twitter4j.StallWarning) {}
    }
    twitterStream.addListener(listener)
    // twitterStream.sample()
    // http://mytwitterid.com/
    val frbayart: Long = 21882051
    val noootsab: Long = 20847695
    val xtordoir: Long = 42385256
    val kensuio: Long = 796666787215667200L

    val accountsToFollow: Array[Long] = Array(noootsab, frbayart, xtordoir, kensuio)
    val keywords: Array[String] = Array("bitcoin", "etherum", "litecoin")
    val filter = new FilterQuery().track(keywords:_*).follow(accountsToFollow:_*)
    print(filter)
    twitterStream.filter(filter)
}

class TweetsCollector(eventhub: ActorRef) extends Actor with ActorLogging {
    //   import org.apache.kafka.clients.producer.ProducerRecord

    override def preStart() = {}

    def receive = {
        case status: twitter4j.Status =>
            val tweet: Tweet = Utils.toData(status)
            eventhub ! tweet
    }
}
