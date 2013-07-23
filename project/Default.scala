// import com.foursquare.recordv2.codegen.plugin.ThriftCodegenPlugin
import com.mojolly.scalate.ScalatePlugin
import com.mojolly.scalate.ScalatePlugin.{ScalateKeys, TemplateConfig}
import sbt._

object Default {
  val all: Seq[Setting[_]] = Seq(
    Keys.target <<= (Keys.name)(name => Path.absolute(file("target") / name)),
    Keys.version := "0.2-SNAPSHOT",
    Keys.organization := "com.foursquare.common"
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

  val scala: Seq[Setting[_]] = all ++ Seq(
    Keys.scalaVersion := "2.9.1",
    Keys.javaOptions ++= Seq(
      "-Dscala.timings=true"),
    Keys.scalacOptions ++= Seq(
      "-Ydependent-method-types",
      "-Xfatal-warnings",
      "-deprecation",
      "-unchecked")
  )

  val scalate: Seq[Setting[_]] = scala ++ ScalatePlugin.scalateSettings ++ Seq(
    ScalateKeys.scalateTemplateConfig in Compile <<= (Keys.baseDirectory in Compile) { base => 
      Seq(
        TemplateConfig(
          scalateTemplateDirectory = base,
          scalateImports = Nil,
          scalateBindings = Nil
        )
      )
    },
    Keys.libraryDependencies ++= (
      ThirdParty.scalate)
  ) 

  // val thrift = Default.scala ++ ThriftCodegenPlugin.thriftSettings ++ Seq(
  //   ThriftCodegenPlugin.thriftCodegenVersion := "0.4-SNAPSHOT",
  //   Keys.sources in ThriftCodegenPlugin.thrift in Compile <<= Keys.baseDirectory.map(base => (base ** "*.thrift").get)
  // )
}
