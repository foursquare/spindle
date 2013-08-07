// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

import sbt._

object Plugins extends Build {
  lazy val root = Project("root", file(".")) dependsOn(
    uri("git://github.com/sbt/sbt-assembly.git#0.9.0")
  )
}
