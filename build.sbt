name := "MVP2"
version := "0.1"
organization := "org.encryfoundation"
scalaVersion := "2.12.6"

val akkaVersion = "2.5.13"
val akkaHttpVersion = "10.0.9"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "org.iq80.leveldb" % "leveldb" % "0.7",
  "javax.xml.bind" % "jaxb-api" % "2.3.0",
  "com.iheart" %% "ficus" % "1.4.2",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.58",
  "org.whispersystems" % "curve25519-java" % "0.5.0",
  "org.rudogma" %% "supertagged" % "1.4",
  "de.heikoseeberger" %% "akka-http-circe" % "1.20.1",
  "org.influxdb" % "influxdb-java" % "2.10",
  "org.apache.commons" % "commons-io" % "1.3.2",
  "commons-net" % "commons-net" % "3.6"
)


resolvers ++= Seq("Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Typesafe maven releases" at "http://repo.typesafe.com/typesafe/maven-releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/")
