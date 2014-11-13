import sbt._
import Keys._

object SpindleBuild extends Build {
  lazy val all =
    Project(
      id = "all",
      base = file("p/all"),
      settings = Defaults.defaultSettings ++ Default.scala
    ).aggregate(
      thriftBase, thriftBson, thriftJson, runtime, thriftDescriptors, parser, codegenRuntime, codegenTemplates,
      codegenBinary, codegenSbtPlugin, testCodegen, testThriftExample, testParser, testCodegenBinary, testRuntime
    )

  lazy val publishable =
    Project(
      id = "publishable",
      base = file("p/publishable"),
      settings = Defaults.defaultSettings ++ Default.scala ++ Seq(
        Keys.publishArtifact := false
      )
    ).aggregate(
      thriftBase, thriftBson, thriftJson, runtime, thriftDescriptors, parser, codegenRuntime, codegenTemplates,
      codegenBinary, codegenSbtPlugin)

  lazy val thriftBase =
    Project(
      id = "common-thrift-base",
      base = file("src/main/java/com/foursquare/common/thrift/base"))
  lazy val thriftBson =
    Project(
      id = "common-thrift-bson",
      base = file("src/main/java/com/foursquare/common/thrift/bson")) dependsOn(thriftBase)
  lazy val thriftJson =
    Project(
      id = "common-thrift-json",
      base = file("src/main/java/com/foursquare/common/thrift/json")) dependsOn(thriftBase)
  lazy val runtime =
    Project(
      id = "spindle-runtime",
      base = file("src/main/scala/com/foursquare/spindle/runtime")) dependsOn(thriftBase, thriftBson, thriftJson)
  lazy val thriftDescriptors =
    Project(
      id = "thrift-descriptors",
      base = file("src/main/thrift/com/twitter/thrift/descriptors")) dependsOn(runtime, thriftBase, thriftJson)
  lazy val parser =
    Project(
      id = "thrift-parser",
      base = file("src/main/scala/com/foursquare/spindle/codegen/parser")) dependsOn(thriftDescriptors)
  lazy val codegenRuntime =
    Project(
      id = "spindle-codegen-runtime",
      base = file("src/main/scala/com/foursquare/spindle/codegen/runtime")) dependsOn(thriftDescriptors)
  lazy val codegenTemplates =
    Project(
      id = "spindle-codegen-templates",
      base = file("src/main/ssp/codegen")) dependsOn(runtime, codegenRuntime)
  lazy val codegenBinary =
    Project(
      id = "spindle-codegen-binary",
      base = file("src/main/scala/com/foursquare/spindle/codegen/binary")) dependsOn(codegenRuntime, codegenTemplates, parser)
  lazy val codegenSbtPlugin =
    Project(
      id = "spindle-codegen-plugin",
      base = file("src/main/scala/com/foursquare/spindle/codegen/plugin"))
  lazy val testCodegen =
    Project(
      id = "spindle-test-codegen",
      base = file("src/test/thrift/com/foursquare/spindle/codegen")) dependsOn(runtime, thriftBase, thriftJson)
  lazy val testThriftExample =
    Project(
      id = "spindle-test-thriftexample",
      base = file("src/test/thrift/com/foursquare/thriftexample")) dependsOn(runtime, thriftBase, thriftJson)
  lazy val testParser =
    Project(
      id = "spindle-test-parser",
      base = file("src/test/scala/com/foursquare/spindle/codegen/parser")) dependsOn(parser)
  lazy val testCodegenBinary =
    Project(
      id = "spindle-test-codegen-binary",
      base = file("src/test/scala/com/foursquare/spindle/codegen/binary")) dependsOn(codegenBinary)
  lazy val testRuntime =
    Project(
      id = "spindle-test-runtime",
      base = file("src/test/scala/com/foursquare/spindle/runtime")) dependsOn(runtime, thriftJson, thriftBson, testThriftExample, testCodegen)
}
