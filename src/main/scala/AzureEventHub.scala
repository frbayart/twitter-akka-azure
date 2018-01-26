package io.kensu.frbayart

import com.typesafe.config.ConfigFactory
import com.microsoft.azure.eventhubs._
import akka.actor.Actor
import io.kensu.frbayart.models._

object AzureEventHub {

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
}

class AzureEventHubActor extends Actor {
    val azureEvent = AzureEventHub.createEvent
    val schema = Utils.avroSchema

    def receive = {
        case tweet: Tweet =>
            println(s"${System.currentTimeMillis},${tweet}")
            val jsonByteArrayMessage = Utils.avroMessage(schema, tweet)
            azureEvent.sendSync(new EventData(jsonByteArrayMessage))
    }
}
