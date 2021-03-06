scalaVersion := "2.9.2"

addSbtPlugin("com.mojolly.scalate" % "xsbt-scalate-generator" % "0.4.2")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.2.5")

addSbtPlugin(
  "com.foursquare" % "spindle-codegen-plugin" % "3.0.0-M6",
  scalaVersion = "2.9.2",
  sbtVersion = "0.12")

