import akka.actor.Actor
import akka.actor.ActorLogging
import twitter4j._
import twitter4j.conf.ConfigurationBuilder
import twitter4j.FilterQuery
import com.typesafe.config.ConfigFactory
import com.microsoft.azure.eventhubs._

// AVRO
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificDatumWriter
import scala.io.Source
import org.apache.avro.io._
import java.io.ByteArrayOutputStream


case class Tweet(
        id: Long,
        userId: Long,
        createdAt: String,
        name: String,
        message: String
)

object TwitterStreamer extends App {
    val conf = ConfigFactory.load()
    val twitterConfig = conf.getConfig("twitter")

    implicit val system = akka.actor.ActorSystem()
    import system.dispatcher
    import akka.actor.Props
    val streamActor = system.actorOf(Props(new TweetsCollector()), "twitter-stream")

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
    // @frbayart : 21882051
    // Thomas : 2653115810
    val follow: Long = 21882051
    val keywords: String = "bitcoin, etherum, litecoin"
    val filter = new FilterQuery().track(keywords).follow(follow).follow(2653115810L)
    twitterStream.filter(filter)
}

object Utils {
    val conf = ConfigFactory.load()
    val eventHubConfig = conf.getConfig("eventhub")


    def createEvent() = {
        val namespaceName: String = eventHubConfig.getString("namespaceName")
        val eventHubName = eventHubConfig.getString("eventHubName")
        val sasKeyName = eventHubConfig.getString("sasKeyName")
        val sasKey = eventHubConfig.getString("sasKey")
        val connStr: ConnectionStringBuilder = new ConnectionStringBuilder(namespaceName, eventHubName, sasKeyName, sasKey)

        val ehClient: EventHubClient = EventHubClient.createFromConnectionStringSync(connStr.toString())
        ehClient
    }

    def avroSchema(): Schema = {
        //Read avro schema file
        // val schema: Schema = new Schema.Parser().parse(SCHEMA_STRING)
        // val schema: Schema = new Parser().parse(Source.fromFile("conf/schema.avsc").mkString)
        new Parser().parse(Source.fromFile("conf/schema.avsc").mkString)
    }

    def avroMessage(schema: Schema, tweet: Tweet): Array[Byte] = {
        // Create avro generic record object
        val genericTweet: GenericRecord = new GenericData.Record(schema)

        genericTweet.put("id", tweet.id)
        genericTweet.put("userid", tweet.userId)
        genericTweet.put("created_at", tweet.createdAt)
        genericTweet.put("name", tweet.name)
        genericTweet.put("message", tweet.message)
        val writer = new SpecificDatumWriter[GenericRecord](schema)
        val out = new ByteArrayOutputStream()
        val encoder: JsonEncoder = EncoderFactory.get().jsonEncoder(schema, out)
        // val encoder: BinaryEncoder = EncoderFactory.get().binaryEncoder(out, null)
        writer.write(genericTweet, encoder)
        encoder.flush()
        out.close()
        out.toByteArray()
    }

}

class TweetsCollector() extends Actor with ActorLogging {
    //   import org.apache.kafka.clients.producer.ProducerRecord

    val producer = Utils.createEvent
    val schema = Utils.avroSchema

    override def preStart() = {}

    def toData(status: twitter4j.Status): Tweet = Tweet(
        status.getId(),
        status.getUser().getId(),
        status.getCreatedAt().toString,
        status.getUser().getName(),
        status.getText()
        )

    def receive = {
        case status: twitter4j.Status =>
            val d = toData(status)
            //if (scala.util.Random.nextDouble < 0.001) list.append(d)

            println(s"${System.currentTimeMillis},${d}")
            //??? //produce
            // new ProducerRecord[String, String]("tweets", null,s"${System.currentTimeMillis},${d}")
            producer.sendSync(new EventData(Utils.avroMessage(schema, d)))
    }
}
