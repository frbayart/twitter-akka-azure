package io.kensu.frbayart

import io.kensu.frbayart.models._
import scala.io.Source
// AVRO
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificDatumWriter
import java.io.ByteArrayOutputStream
import org.apache.avro.io._

object Utils {

    def toData(status: twitter4j.Status): Tweet = Tweet(
        status.getId(),
        status.getUser().getId(),
        status.getCreatedAt().toString,
        status.getUser().getName(),
        status.getText()
        )

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
