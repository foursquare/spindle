import sbt._
import Keys._

object SpindleBuild extends Build {
  lazy val all =
    Project(
      id = "all",
      base = file("all"),
      settings = Defaults.defaultSettings ++ Default.scala
    ).aggregate(
      thriftBase, thriftBson, thriftJson, runtime, thriftDescriptors, parser, codegenRuntime, codegenTemplates,
      codegenBinary, codegenSbtPlugin, testCodegen, testThriftExample, testParser, testRuntime
    )

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
      base = file("src/main/scala/com/foursquare/spindle/runtime"))
  lazy val thriftDescriptors =
    Project(
      id = "thrift-descriptors",
      base = file("src/main/thrift/com/twitter/thrift/descriptors")) dependsOn(runtime, thriftBase, thriftJson)
  lazy val parser =
    Project(
      id = "thrift-parser-2",
      base = file("src/main/scala/com/foursquare/spindle/codegen/parser")) dependsOn(thriftDescriptors)
  lazy val codegenRuntime =
    Project(
      id = "thrift-codegen-runtime",
      base = file("src/main/scala/com/foursquare/spindle/codegen/runtime")) dependsOn(thriftDescriptors)
  lazy val codegenTemplates =
    Project(
      id = "thrift-codegen-templates",
      base = file("src/main/ssp/codegen")) dependsOn(runtime, codegenRuntime)
  lazy val codegenBinary =
    Project(
      id = "thrift-codegen-binary",
      base = file("src/main/scala/com/foursquare/spindle/codegen/binary")) dependsOn(codegenRuntime, codegenTemplates, parser)
  lazy val codegenSbtPlugin =
    Project(
      id = "thrift-codegen-plugin",
      base = file("src/main/scala/com/foursquare/spindle/codegen/plugin"))
  lazy val testCodegen =
    Project(
      id = "test-codegen",
      base = file("src/test/thrift/com/foursquare/spindle/codegen")) dependsOn(runtime, thriftBase, thriftJson)
  lazy val testThriftExample =
    Project(
      id = "test-thriftexample",
      base = file("src/test/thrift/com/foursquare/thriftexample")) dependsOn(runtime, thriftBase, thriftJson)
  lazy val testParser =
    Project(
      id = "test-parser",
      base = file("src/test/scala/com/foursquare/spindle/codegen/parser")) dependsOn(parser)
  lazy val testRuntime =
    Project(
      id = "test-runtime",
      base = file("src/test/scala/com/foursquare/spindle/runtime")) dependsOn(runtime, thriftJson, thriftBson, testThriftExample, testCodegen)
}
