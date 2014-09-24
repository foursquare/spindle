// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

import sbt._

object ThirdParty {
  def argot(v: String) =
    if (v.startsWith("2.9")) argot29
    else if (v.startsWith("2.10")) argot210
    else sys.error("Unsupported Scala version for Argot")
  val argot29 = Seq(
    "org.clapper" %% "argot" % "0.4")
  val argot210 = Seq(
    "org.clapper" %% "argot" % "1.0.1")
  val commonsIo = Seq(
    "commons-io" % "commons-io" % "2.1")
  def finagleThrift(v: String) =
    if (v.startsWith("2.9")) finagleThrift29
    else if (v.startsWith("2.10")) finagleThrift210
    else sys.error("Unsupported Scala version for finagle-thrift")
  val finagleThrift29 = Seq(
    "com.twitter" % "finagle-thrift_2.9.2" % "6.3.0")
  val finagleThrift210 = Seq(
    "com.twitter" %% "finagle-thrift" % "6.16.0")
  val jackson = Seq(
    "org.codehaus.jackson" % "jackson-core-asl" % "1.9.8",
    "org.codehaus.jackson" % "jackson-mapper-asl" % "1.9.8",
    "org.codehaus.jackson" % "jackson-xc" % "1.9.8")
  val jodaTime = Seq(
    "joda-time" % "joda-time" % "2.1",
    "org.joda" % "joda-convert" % "1.2")
  val junit = Seq(
    "junit" % "junit" % "4.11" withSources(),
    "com.novocode" % "junit-interface" % "0.10")
  val mongodb = Seq(
    "org.mongodb" % "mongo-java-driver" % "2.9.3")
  def parboiledScala(v: String) =
    if (v.startsWith("2.9")) parboiledScala29
    else if (v.startsWith("2.10")) parboiledScala210
    else sys.error("Unsupported Scala version for Parboiled")
  val parboiledScala29 = Seq(
    "org.parboiled" % "parboiled-scala_2.9.2" % "1.1.4")
  val parboiledScala210 = Seq(
    "org.parboiled" %% "parboiled-scala" % "1.1.4")
  val phonenumbers = Seq(
    "com.googlecode.libphonenumber" % "libphonenumber" % "5.6")
  val rogueField = Seq(
    "com.foursquare" %% "rogue-field" % "2.2.1")
  val scalajCollection = Seq(
    "org.scalaj" %% "scalaj-collection" % "1.5")
  val scalajTime = Seq(
    "org.scalaj" %% "scalaj-time" % "0.7")
  def scalate(v: String) =
    if (v.startsWith("2.9")) scalate29
    else if (v.startsWith("2.10")) scalate210
    else sys.error("Unsupported Scala version for Scalate")
  val scalate29 = Seq(
    "org.fusesource.scalate" % "scalate-core_2.9" % "1.6.1" exclude(
      "org.scala-lang", "scala-compiler"))
  val scalate210 = Seq(
    "org.fusesource.scalate" %% "scalate-core" % "1.6.1" exclude(
      "org.scala-lang", "scala-compiler"))
  val slf4jNoLogging = Seq(
    "org.slf4j" % "slf4j-nop" % "1.6.4")
  def specs(v: String) =
    if (v.startsWith("2.9")) specs29
    else if (v.startsWith("2.10")) specs210
    else sys.error("Unsupported Scala version for Specs")
  val specs29 = Seq(
    "org.scala-tools.testing" % "specs_2.9.1" % "1.6.9" withSources())
  val specs210 = Seq(
    "org.scala-tools.testing" %% "specs" % "1.6.9" withSources())
  val thrift = Seq(
    "org.apache.thrift" % "libthrift" % "0.9.0")
}
