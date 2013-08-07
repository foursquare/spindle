import com.foursquare.spindle.codegen.plugin.ThriftCodegenPlugin
import com.mojolly.scalate.ScalatePlugin
import com.mojolly.scalate.ScalatePlugin.{ScalateKeys, TemplateConfig}
import sbt._

object Default {
  val all: Seq[Setting[_]] = Seq(
    Keys.target <<= (Keys.name)(name => Path.absolute(file("target") / name)),
    Keys.version := "1.0.0-SNAPSHOT",
    Keys.organization := "com.foursquare",
    Keys.scalaVersion := "2.9.1",
    Keys.crossScalaVersions := Seq("2.9.1", "2.9.2")
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

  val commonJava: Seq[Setting[_]] = Default.all ++ Seq(
    Keys.crossPaths := false
  )

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
    Keys.sourceDirectory in ThriftCodegenPlugin.thrift in Compile <<= (Keys.baseDirectory)(identity),
    ThriftCodegenPlugin.thriftCodegenRuntimeLibs := ThirdParty.scalajCollection,
    ThriftCodegenPlugin.thriftCodegenBinaryLibs <<= (ThriftCodegenPlugin.thriftCodegenVersion)(v =>
      Seq(
        "com.foursquare" % "common-thrift-base" % v,
        "com.foursquare" % "common-thrift-json" % v,
        "com.foursquare" % "spindle-codegen-binary_2.9.1" % v
      ) ++ ThirdParty.scalajCollection
    )
  )

  val test = Default.scala ++ Seq(
    Keys.libraryDependencies ++= (ThirdParty.junit ++ ThirdParty.specs).map(_ % "test"),
    Keys.sourcesInBase := false,
    Keys.scalaSource in Test <<= (Keys.baseDirectory)(identity)
  )
}
