import sbt._
import Keys._

object Recordv2Build extends Build {
  lazy val all = Project(id = "all", base = file("all")) aggregate(
    thriftBase, thriftBson, thriftJson, runtime, /*thriftDescriptors,*/ thriftDescriptorsScala, parser2, codegenRuntime,
    codegenBinary, templates, codegenSbtPlugin
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
      id = "recordv2-runtime",
      base = file("src/main/scala/com/foursquare/recordv2/runtime"))
  // lazy val thriftDescriptors =
  //   Project(
  //     id = "thrift-descriptors",
  //     base = file("src/main/thrift/com/twitter/thrift/descriptors")) dependsOn(runtime, thriftBase, thriftJson)
  lazy val thriftDescriptorsScala =
    Project(
      id = "thrift-descriptors-scala",
      base = file("src/main/scala/com/twitter/thrift/descriptors")) dependsOn(runtime, thriftBase, thriftJson)
  lazy val parser2 =
    Project(
      id = "thrift-parser-2",
      base = file("src/main/scala/com/foursquare/recordv2/parser2")) dependsOn(thriftDescriptorsScala)
  lazy val codegenRuntime =
    Project(
      id = "thrift-codegen-runtime",
      base = file("src/main/scala/com/foursquare/recordv2/codegen/runtime")) dependsOn(thriftDescriptorsScala)
  lazy val codegenBinary =
    Project(
      id = "thrift-codegen-binary",
      base = file("src/main/scala/com/foursquare/recordv2/codegen/binary")) dependsOn(codegenRuntime, parser2)
  lazy val codegenSbtPlugin =
    Project(
      id = "thrift-codegen-plugin",
      base = file("src/main/scala/com/foursquare/recordv2/codegen/plugin"))
  lazy val templates =
    Project(
      id = "codegen-templates",
      base = file("src/main/ssp/codegen")) dependsOn(runtime, codegenRuntime)
}
