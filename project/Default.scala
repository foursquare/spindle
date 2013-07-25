import com.foursquare.recordv2.codegen.plugin.ThriftCodegenPlugin
import com.mojolly.scalate.ScalatePlugin
import com.mojolly.scalate.ScalatePlugin.{ScalateKeys, TemplateConfig}
import sbt._

object Default {
  val all: Seq[Setting[_]] = Seq(
    Keys.target <<= (Keys.name)(name => Path.absolute(file("target") / name)),
    Keys.version := "0.10-SNAPSHOT",
    Keys.organization := "com.foursquare.common",
    Keys.scalaVersion := "2.9.1",
    Keys.crossScalaVersions := Seq("2.9.1", "2.9.2")
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

  val scala: Seq[Setting[_]] = Default.all ++ Seq(
    Keys.scalacOptions ++= Seq(
      "-Ydependent-method-types",
      "-Xfatal-warnings",
      "-deprecation",
      "-unchecked")
  )

  val scalate: Seq[Setting[_]] = Default.scala ++ ScalatePlugin.scalateSettings ++ Seq(
    ScalateKeys.scalateTemplateConfig in Compile <<= (Keys.baseDirectory in Compile) { base => 
      Seq(
        TemplateConfig(
          scalateTemplateDirectory = base,
          scalateImports = Nil,
          scalateBindings = Nil
        )
      )
    },
    Keys.unmanagedResourceDirectories in Compile <<= (Keys.baseDirectory in Compile)(dir => Seq(dir)),
    Keys.includeFilter in Keys.unmanagedResources := "*.ssp",
    Keys.libraryDependencies ++= (
      ThirdParty.scalate)
  ) 

  val thrift = Default.scala ++ ThriftCodegenPlugin.thriftSettings ++ Seq(
    ThriftCodegenPlugin.thriftCodegenLibs := Seq(
      "com.foursquare.common" % "thrift-codegen-binary_2.9.1" % "0.4-SNAPSHOT"
    ),
    Keys.sourceDirectory in ThriftCodegenPlugin.thrift in Compile <<= (Keys.baseDirectory)(identity),
    ThriftCodegenPlugin.thriftCodegenIncludes := Seq(file("src/main/thrift")),
    Keys.sources in ThriftCodegenPlugin.thrift in Compile <<= Keys.baseDirectory.map(base => (base ** "*.thrift").get),
    Keys.libraryDependencies ++= (
      ThirdParty.jackson ++
      ThirdParty.thrift ++
      ThirdParty.scalajCollection)
  )
}

