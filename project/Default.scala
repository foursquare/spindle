// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

import com.foursquare.spindle.codegen.plugin.ThriftCodegenPlugin
import com.mojolly.scalate.ScalatePlugin
import com.mojolly.scalate.ScalatePlugin.{ScalateKeys, TemplateConfig}
import sbtbuildinfo.Plugin._
import sbt._

object Default {
  lazy val IvyDefaultConfiguration = config("default") extend(Compile)
  val all: Seq[Setting[_]] = Seq(
    Keys.target <<= (Keys.name)(name => Path.absolute(file("target") / name)),
    Keys.version := "3.0.0-M7",
    Keys.organization := "com.foursquare",
    Keys.ivyConfigurations += IvyDefaultConfiguration,
    Keys.publishMavenStyle := true,
    Keys.publishArtifact in Test := false,
    Keys.pomIncludeRepository := { _ => false },
    Keys.publishTo <<= (Keys.version) { v =>
      val nexus = "https://oss.sonatype.org/"
      if (v.endsWith("-SNAPSHOT"))
        Some("snapshots" at nexus+"content/repositories/snapshots")
      else
        Some("releases" at nexus+"service/local/staging/deploy/maven2")
    },
    Keys.pomExtra := (
      <url>http://github.com/foursquare/spindle</url>
      <licenses>
        <license>
          <name>Apache</name>
          <url>http://www.opensource.org/licenses/Apache-2.0</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:foursquare/spindle.git</url>
        <connection>scm:git:git@github.com:foursquare/spindle.git</connection>
      </scm>
      <developers>
        <developer>
          <id>jorgeortiz85</id>
          <name>Jorge Ortiz</name>
          <url>http://github.com/jorgeortiz85</url>
        </developer>
      </developers>),
    Keys.credentials ++= {
      val sonatype = ("Sonatype Nexus Repository Manager", "oss.sonatype.org")
      def loadMavenCredentials(file: java.io.File) : Seq[Credentials] = {
        xml.XML.loadFile(file) \ "servers" \ "server" map (s => {
          val host = (s \ "id").text
          val realm = if (host == sonatype._2) sonatype._1 else "Unknown"
          Credentials(realm, host, (s \ "username").text, (s \ "password").text)
        })
      }
      val ivyCredentials   = Path.userHome / ".ivy2" / ".credentials"
      val mavenCredentials = Path.userHome / ".m2"   / "settings.xml"
      (ivyCredentials.asFile, mavenCredentials.asFile) match {
        case (ivy, _) if ivy.canRead => Credentials(ivy) :: Nil
        case (_, mvn) if mvn.canRead => loadMavenCredentials(mvn)
        case _ => Nil
      }
    }
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

  val commonJava: Seq[Setting[_]] = Default.all ++ Seq(
    Keys.crossPaths := false,
    Keys.javacOptions := Seq(
      "-source", "1.6",
      "-target", "1.6"),
    Keys.javacOptions in Keys.doc := Nil,
    Keys.autoScalaLibrary := false
  )

  val scala: Seq[Setting[_]] = Default.all ++ Seq(
    Keys.scalaVersion := "2.9.2",
    Keys.crossScalaVersions := Seq("2.9.2", "2.10.4"),
    Keys.scalacOptions <++= (Keys.scalaVersion).map(v => {
      val opts =
        Seq(
          "-deprecation",
          "-unchecked")
      if (v.startsWith("2.9.")) {
        opts ++ Seq(
          "-Ydependent-method-types",
          "-Xfatal-warnings")
      } else {
        opts
      }
    })
  ) ++ buildInfoSettings ++ Seq(
    Keys.sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys := Seq[BuildInfoKey](Keys.version),
    buildInfoPackage := "com.foursquare.spindle",
    buildInfoObject := "Info"
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
    Keys.libraryDependencies <++= (Keys.scalaVersion)(v =>
      ThirdParty.scalate(v))
  )

  val thriftBootstrap = Default.scala ++ ThriftCodegenPlugin.thriftSettings ++ Seq(
    Keys.sourceDirectory in ThriftCodegenPlugin.thrift in Compile <<= (Keys.baseDirectory)(identity),
    Keys.scalaBinaryVersion in ThriftCodegenPlugin.thrift := "2.9.2",
    ThriftCodegenPlugin.thriftCodegenVersion := "3.0.0-M6",
    ThriftCodegenPlugin.thriftCodegenRuntimeLibs := ThirdParty.scalajCollection,
    ThriftCodegenPlugin.thriftCodegenTemplate := file("src/main/ssp/codegen/scala/record.ssp").absolutePath,
    ThriftCodegenPlugin.thriftCodegenJavaTemplate := file("src/main/ssp/codegen/javagen/record.ssp").absolutePath,
    Keys.ivyScala <<= (Keys.ivyScala)(_.map(_.copy(checkExplicit = false, filterImplicit = false, overrideScalaVersion =false))),
    Keys.update <<= (Keys.ivyModule, Keys.thisProjectRef, Keys.updateConfiguration, Keys.cacheDirectory, Keys.transitiveUpdate,
        Keys.executionRoots, Keys.resolvedScoped, Keys.skip in Keys.update, Keys.streams) map {
      (module, ref, config, cacheDirectory, reports, roots, resolved, skip, s) =>
        val depsUpdated = reports.exists(!_.stats.cached)
        val isRoot = roots contains resolved
        Classpaths.cachedUpdate(cacheDirectory / "update", Project.display(ref), module, config, None, skip = skip, force = isRoot, depsUpdated = depsUpdated, log = s.log)
    } tag(Tags.Update, Tags.Network),
    ThriftCodegenPlugin.thriftCodegenBinaryLibs <<=
      (ThriftCodegenPlugin.thriftCodegenVersion, Keys.scalaBinaryVersion in ThriftCodegenPlugin.thrift)((cv, bv) =>
        Seq(
          "com.foursquare" % "common-thrift-base" % cv,
          "com.foursquare" % "common-thrift-json" % cv,
          "com.foursquare" % ("spindle-codegen-binary_" + bv) % cv,
          "org.scalaj" % ("scalaj-collection_" + bv) % "1.5",
          "org.scala-lang" % "scala-library" % bv,
          "org.scala-lang" % "scala-compiler" % bv
        )
      )
  )

  val thriftTestLocal = Default.scala ++ Seq(
    Keys.libraryDependencies <++= (Keys.scalaVersion) (v =>
      ThirdParty.jackson ++
      ThirdParty.thrift ++
      ThirdParty.scalajCollection ++
      ThirdParty.finagleThrift(v)),
    Keys.sourceGenerators in Compile <+=
      (Keys.fullClasspath in (SpindleBuild.codegenBinary, Runtime), Keys.runner in SpindleBuild.codegenBinary,
          Keys.baseDirectory, Keys.sourceManaged, Keys.crossTarget, Keys.configuration in Compile, Keys.streams) map {
            (cp, r, in, out, targetDir, conf, s) =>
        val template = file("src/main/ssp/codegen/scala/record.ssp")
        val javaTemplate = file("src/main/ssp/codegen/javagen/record.ssp")
        val extension = "scala"
        val workingDir = targetDir / (Defaults.prefix(conf.name) + "scalate.d")
        out.mkdirs()
        val thriftSources: Seq[File] = ((in ** "*.thrift").get)
        r.run(
          "com.foursquare.spindle.codegen.binary.ThriftCodegen",
          cp.files,
          Seq(
            "--template", template.absolutePath,
            "--java_template", javaTemplate.absolutePath,
            "--extension", extension,
            "--namespace_out", out.absolutePath,
            "--thrift_include", "src/test/thrift",
            "--working_dir", workingDir.absolutePath) ++
          thriftSources.map(_.absolutePath),
          s.log
        ).foreach(error)
        (out ** "*.java").get ++ (out ** "*.scala").get
      }
  )

  val test = Default.scala ++ Seq(
    Keys.libraryDependencies <++= (Keys.scalaVersion)(v => (ThirdParty.junit ++ ThirdParty.specs(v)).map(_ % "test")),
    Keys.sourcesInBase := false,
    Keys.scalaSource in Test <<= (Keys.baseDirectory)(identity)
  )
}
