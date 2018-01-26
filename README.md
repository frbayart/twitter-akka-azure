# twitter-akka-azure

Read Twitter Streaming API with Akka and send it to Azure EventHub

## Usage

You have to put your Twitter App and Azure credentials in `conf/application.conf` (see conf/application.conf.example)

Running:

```sbt -Dconfig.file=conf/application.conf ~run```
