import sbt._

object ThirdParty {
  val argot = Seq(
    "org.clapper" %% "argot" % "0.4")
  val commonsIo = Seq(
    "commons-io" % "commons-io" % "2.1")
  val finagleThrift = Seq(
    "com.twitter" % "finagle-thrift" % "6.3.0")
  val jackson = Seq(
    "org.codehaus.jackson" % "jackson-core-asl" % "1.9.8",
    "org.codehaus.jackson" % "jackson-mapper-asl" % "1.9.8",
    "org.codehaus.jackson" % "jackson-xc" % "1.9.8")
  val jodaTime = Seq(
    "joda-time" % "joda-time" % "2.0_2012b",
    "org.joda" % "joda-convert" % "1.2")
  val mongodb = Seq(
    "org.mongodb" % "mongo-java-driver" % "2.9.3")
  val parboiledScala = Seq(
    "org.parboiled" % "parboiled-scala_2.9.2" % "1.1.4")
  val rogueField = Seq(
    "com.foursquare" %% "rogue-field" % "2.2.0")
  val scalaIo = Seq(
    "com.github.scala-incubator.io" % "scala-io-core_2.9.1" % "0.3.0",
    "com.github.scala-incubator.io" % "scala-io-file_2.9.1" % "0.3.0")
  val scalajCollection = Seq(
    "org.scalaj" %% "scalaj-collection" % "1.5")
  val scalajTime = Seq(
    "org.scalaj" %% "scalaj-time" % "0.7")
  val scalate = Seq(
    "org.fusesource.scalate" % "scalate-core_2.9" % "1.6.1" exclude(
      "org.scala-lang", "scala-compiler"))
  val slf4jNoLogging = Seq(
    "org.slf4j" % "slf4j-nop" % "1.6.4")
  val thrift = Seq(
    "org.apache.thrift" % "libthrift" % "0.9.0")
}
