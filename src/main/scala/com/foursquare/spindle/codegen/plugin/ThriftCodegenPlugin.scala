// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.codegen.plugin

import sbt._
import sbt.Fork.ForkJava
import sbt.Keys.TaskStreams
import java.io.File

object ThriftCodegenPlugin extends Plugin {
  val Thrift = config("thrift").hide

  val thrift = TaskKey[Seq[File]]("thrift", "Generate Scala sources from Thrift files(s)")
  val thriftCodegenVersion = SettingKey[String]("thrift-codegen-version", "Version of Thrift codegen to use.")
  val thriftCodegenBinaryLibs = SettingKey[Seq[ModuleID]]("thrift-codegen-binary-libs", "Version of Thrift codegen binary to use.")
  val thriftCodegenRuntimeLibs = SettingKey[Seq[ModuleID]]("thrift-codegen-runtime-libs", "Libraries needed for Thrift generated code.")
  val thriftCodegenIncludes = SettingKey[Seq[File]]("thrift-codegen-includes", "Directories to include in Thrift dependency resolution.")
  val thriftCodegenTemplate = SettingKey[String]("thrift-codegen-template", "Template to use for generating Scala code.")
  val thriftCodegenJavaTemplate = SettingKey[String]("thrift-codegen-java-template", "Template to use for generating Java code.")
  val thriftCodegenAllowReload = SettingKey[Boolean]("thrift-codegen-allow-reload", "Allow reloading of codegen templates.")
  val thriftCodegenWorkingDir = SettingKey[File]("thrift-codegen-working-dir", "Directory to use for caching compiled Scalate templates.")

  val thriftSettings = Seq[Project.Setting[_]](
    Keys.ivyConfigurations += Thrift,
    Keys.libraryDependencies <++= (thriftCodegenBinaryLibs, thriftCodegenRuntimeLibs)((binary, runtime) => {
      runtime ++ binary.map(_ % "thrift")
    }),
    thriftCodegenTemplate := "scala/record.ssp",
    thriftCodegenJavaTemplate := "javagen/record.ssp",
    thriftCodegenAllowReload := false,
    thriftCodegenVersion := "3.0.0-M5.1",
    thriftCodegenBinaryLibs <<= (thriftCodegenVersion, Keys.scalaBinaryVersion in thrift)((cv, bv) =>
        Seq("com.foursquare" % ("spindle-codegen-binary_" + bv) % cv)
    ),
    thriftCodegenRuntimeLibs <<= (thriftCodegenVersion, Keys.scalaVersion)((v, sv) => {
      def finagleThrift(v: String): ModuleID = {
        if (v.startsWith("2.9")) {
          "com.twitter" % "finagle-thrift_2.9.2" % "6.3.0"
        } else if (v.startsWith("2.10")) {
          "com.twitter" %% "finagle-thrift" % "6.3.0"
        } else {
          sys.error("Unsupported Scala version for finagle-thrift")
        }
      }

      Seq(
        "com.foursquare" %% "spindle-runtime"    % v,
        "com.foursquare" %  "common-thrift-base" % v,
        "com.foursquare" %  "common-thrift-json" % v,
        "org.scalaj"     %% "scalaj-collection"  % "1.5"
      ) :+ finagleThrift(sv)
    })
  ) ++ thriftSettingsIn(Compile) ++ thriftSettingsIn(Test)

  def thriftSettingsIn(conf: Configuration): Seq[Project.Setting[_]] =
    inConfig(conf)(thriftSettings0) ++ Seq(
      Keys.clean <<= Keys.clean.dependsOn(Keys.clean in thrift in conf),
      Keys.sourceDirectory in thrift in conf <<= (Keys.sourceDirectory in conf)(_ / "thrift"),
      Keys.sources in thrift in conf <<= (Keys.sourceDirectory in thrift in conf).map(dir => (dir ** "*.thrift").get),
      thriftCodegenIncludes in conf <<= (Keys.sourceDirectory in thrift in conf)(dir => Seq(dir))
    )

  private def thriftSettings0 = Seq[Project.Setting[_]](
    thriftCodegenWorkingDir <<= (Keys.crossTarget, Keys.configuration) { (outDir, conf) =>
      outDir / (Defaults.prefix(conf.name) + "scalate.d")
    },
    Keys.sourceManaged in thrift ~= (_ / "thrift"), // e.g. /target/scala-2.8.1.final/src_managed/main/thrift
    thrift                       <<= (Keys.javaHome, Keys.classpathTypes in thrift, Keys.update,
                                  Keys.sources in thrift, thriftCodegenTemplate, thriftCodegenJavaTemplate, Keys.sourceManaged in thrift,
                                  thriftCodegenIncludes, thriftCodegenAllowReload, thriftCodegenWorkingDir,
                                  Keys.resolvedScoped, Keys.streams).map(thriftCompile),
    Keys.sourceGenerators        <+= thrift,
    Keys.clean in thrift         <<= (Keys.sourceManaged in thrift, thriftCodegenWorkingDir, Keys.resolvedScoped,
                                  Keys.streams).map(thriftClean)
  )

  /**
   * @return the .scala files in `sourceManaged` after compilation.
   */
  private def thriftCompile(
      javaHome: Option[File],
      classpathTypes: Set[String],
      updateReport: UpdateReport,
      thriftSources: Seq[File],
      template: String,
      javaTemplate: String,
      sourceManaged: File,
      includes: Seq[File],
      allowReload: Boolean,
      workingDir: File,
      resolvedScoped: Project.ScopedKey[_],
      streams: TaskStreams
  ): Seq[File] = {
    import streams.log
    def generated = (sourceManaged ** "*.java").get ++ (sourceManaged ** "*.scala").get

    val shouldProcess = (thriftSources, generated) match {
      case (Seq(), _) => log.debug("No sources, skipping."); false
      case (_, Seq()) => log.debug("No products, generating."); true
      case (ins, outs) =>
        val inLastMod = ins.map(_.lastModified()).max
        val outLasMod = outs.map(_.lastModified()).min
        log.debug("Sources last modified: %s. Products last modified: %s.".format(inLastMod, outLasMod))
        outLasMod < inLastMod
    }

    lazy val options: Seq[String] = {
      import File.pathSeparator
      def jars(config: Configuration): Seq[File] = Classpaths.managedJars(config, classpathTypes, updateReport).map(_.data)
      val mainJars = jars(Thrift)
      val jvmCpOptions = Seq("-classpath", mainJars.mkString(pathSeparator))
      val thriftSourcePaths = thriftSources.map(_.absolutePath)
      val mainClass = "com.foursquare.spindle.codegen.binary.ThriftCodegen"
      val appOptions = Seq(
        "--template", template,
        "--java_template", javaTemplate,
        "--extension", "scala",
        "--namespace_out", sourceManaged.absolutePath,
        "--thrift_include", includes.map(_.absolutePath).mkString(":"),
        "--working_dir", workingDir.absolutePath
      ) ++ (
        if (allowReload) Seq("--allow_reload") else Nil
      )

      jvmCpOptions ++ List(mainClass) ++ appOptions ++ thriftSourcePaths
    }

    if (shouldProcess) {
      sourceManaged.mkdirs()
      log.info("Compiling %d Thrift file(s) in %s".format(thriftSources.size, Project.displayFull(resolvedScoped)))
      log.debug("Thrift java command line: " + options.mkString("\n"))
      val returnCode = (new ForkJava("java")).apply(javaHome, options, log)
      if (returnCode != 0) sys.error("Non zero return code from thrift [%d]".format(returnCode))
    } else {
      log.debug("No sources newer than products, skipping.")
    }

    generated
  }

  private def thriftClean(
      sourceManaged: File,
      workingDir: File,
      resolvedScoped: Project.ScopedKey[_],
      streams: TaskStreams
  ): Unit = {
    import streams.log
    val scalaFilesToDelete = (sourceManaged ** "*.scala").get
    val scalateFilesToDelete = (workingDir ***).get
    val filesToDelete = scalaFilesToDelete ++ scalateFilesToDelete
    log.debug("Cleaning Files:\n%s".format(filesToDelete.mkString("\n")))
    if (scalaFilesToDelete.nonEmpty) {
      log.info("Cleaning %d Thrift generated files in %s".format(scalaFilesToDelete.size, Project.displayFull(resolvedScoped)))
      IO.delete(scalaFilesToDelete)
    }
    if (scalateFilesToDelete.nonEmpty) {
      log.info("Cleaning %d Scalate generated files.".format(scalateFilesToDelete.size))
      IO.delete(scalateFilesToDelete)
    }
  }
}
