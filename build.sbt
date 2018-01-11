name := "twitter-akka-azure"

version := "1.0"

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
	"org.twitter4j" % "twitter4j-stream" % "4.0.4",
	"com.typesafe.akka" %% "akka-stream" % "2.5.8",
	"com.typesafe.akka" %% "akka-http" % "10.0.11",
	// "com.hunorkovacs" %% "koauth" % "2.0.0",
	"org.json4s" %%  "json4s-native" % "3.5.3",
	"com.microsoft.azure" % "azure-eventhubs" % "0.15.1",
	"org.apache.avro"  %  "avro"  %  "1.8.2"
)

// AVRO TO JSON
// val circeVersion = "0.9.0"
// libraryDependencies ++= Seq(
//   "io.circe" %% "circe-core",
//   "io.circe" %% "circe-generic",
//   "io.circe" %% "circe-parser"
// ).map(_ % circeVersion)
