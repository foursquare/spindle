scalaVersion := "2.9.1"

addSbtPlugin("com.mojolly.scalate" % "xsbt-scalate-generator" % "0.4.2")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin(
  "com.foursquare" % "spindle-codegen-plugin" % "0.18-SNAPSHOT",
  scalaVersion = "2.9.1",
  sbtVersion = "0.12")
