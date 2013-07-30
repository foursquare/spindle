package com.foursquare.recordv2.codegen.plugin

import sbt._
import sbt.Fork.ForkJava
import sbt.Keys.TaskStreams
import java.io.File

object ThriftCodegenPlugin extends Plugin {
  val Thrift   = config("thrift").hide

  val thrift = TaskKey[Seq[File]]("thrift", "Generate Scala sources from Thrift files(s)")
  val thriftCodegenVersion = SettingKey[String]("thrift-codegen-version", "Version of Thrift codegen to use.")
  val thriftCodegenBinaryLibs = SettingKey[Seq[ModuleID]]("thrift-codegen-binary-libs", "Version of Thrift codegen binary to use.")
  val thriftCodegenRuntimeLibs = SettingKey[Seq[ModuleID]]("thrift-codegen-runtime-libs", "Libraries needed for Thrift generated code.")
  val thriftCodegenIncludes = SettingKey[Seq[File]]("thrift-codegen-includes", "Directories to include in Thrift dependency resolution.")
  val thriftCodegenTemplate = SettingKey[String]("thrift-codegen-template", "Template to use for generating code.")
  val thriftCodegenAllowReload = SettingKey[Boolean]("thrift-codegen-allow-reload", "Allow reloading of codegen templates.")

  val thriftSettings     = Seq[Project.Setting[_]](
    Keys.ivyConfigurations += Thrift,
    Keys.libraryDependencies <++= (thriftCodegenBinaryLibs, thriftCodegenRuntimeLibs)((binary, runtime) => {
      runtime ++ binary.map(_ % "thrift")
    }),
    thriftCodegenTemplate := "scala/record.ssp",
    thriftCodegenAllowReload := false,
    thriftCodegenVersion := "0.12-SNAPSHOT",
    thriftCodegenBinaryLibs <<= (thriftCodegenVersion)(v => Seq("com.foursquare.common" %% "thrift-codegen-binary" % v)),
    thriftCodegenRuntimeLibs <<= (thriftCodegenVersion)(v => Seq(
      "com.twitter" % "finagle-thrift" % "6.3.0",
      "com.foursquare.common" %% "recordv2-runtime" % v,
      "com.foursquare.common" %% "common-thrift-base" % v,
      "com.foursquare.common" %% "common-thrift-json" % v,
      "org.scalaj" %% "scalaj-collection" % "1.5"
    ))
  ) ++ thriftSettingsIn(Compile) ++ thriftSettingsIn(Test)

  def thriftSettingsIn(conf: Configuration): Seq[Project.Setting[_]] =
    inConfig(conf)(thriftSettings0) ++ Seq(
      Keys.clean <<= Keys.clean.dependsOn(Keys.clean in thrift in conf),
      Keys.sourceDirectory in thrift in conf <<= (Keys.sourceDirectory in conf)(_ / "thrift"),
      Keys.sources in thrift in conf <<= (Keys.sourceDirectory in thrift in conf).map(dir => (dir ** "*.thrift").get),
      thriftCodegenIncludes in conf <<= (Keys.sourceDirectory in thrift in conf)(dir => Seq(dir))
    )

  private def thriftSettings0 = Seq[Project.Setting[_]](
    Keys.sourceManaged in thrift ~= (_ / "thrift"), // e.g. /target/scala-2.8.1.final/src_managed/main/thrift
    thrift                       <<= (Keys.javaHome, Keys.classpathTypes in thrift, Keys.update,
                                  Keys.sources in thrift, thriftCodegenTemplate, Keys.sourceManaged in thrift,
                                  thriftCodegenIncludes, thriftCodegenAllowReload, Keys.resolvedScoped, Keys.streams
                                  ).map(thriftCompile),
    Keys.sourceGenerators        <+= thrift,
    Keys.clean in thrift         <<= (Keys.sourceManaged in thrift, Keys.resolvedScoped, Keys.streams).map(thriftClean)
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
      sourceManaged: File,
      includes: Seq[File],
      allowReload: Boolean,
      resolvedScoped: Project.ScopedKey[_],
      streams: TaskStreams
  ): Seq[File] = {
    import streams.log
    def generated = (sourceManaged ** "*.scala").get

    val shouldProcess = (thriftSources, generated) match {
      case (Seq(), _)  => log.debug("No sources, skipping."); false
      case (_, Seq())  => log.debug("No products, generating."); true
      case (ins, outs) =>
        val inLastMod = ins.map(_.lastModified()).max
        val outLasMod = outs.map(_.lastModified()).min
        log.debug("Sources last modified: %s. Products last modified: %s.".format(inLastMod, outLasMod))
        outLasMod < inLastMod
    }

    lazy val options: Seq[String] = {
      import File.pathSeparator
      def jars(config: Configuration): Seq[File] = Classpaths.managedJars(config, classpathTypes, updateReport).map(_.data)
      val mainJars        = jars(Thrift)
      val jvmCpOptions    = Seq("-classpath", mainJars.mkString(pathSeparator))
      val thriftSourcePaths  = thriftSources.map(_.absolutePath)
      val mainClass  = "com.foursquare.recordv2.codegen.binary.ThriftCodegen"
      val appOptions = Seq(
        "--template", template,
        "--extension", "scala",
        "--namespace_out", sourceManaged.absolutePath,
        "--thrift_include", includes.map(_.absolutePath).mkString(":")
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

  private def thriftClean(sourceManaged: File, resolvedScoped: Project.ScopedKey[_], streams: TaskStreams) {
    import streams.log
    val filesToDelete = (sourceManaged ** "*.scala").get
    log.debug("Cleaning Files:\n%s".format(filesToDelete.mkString("\n")))
    if (filesToDelete.nonEmpty) {
      log.info("Cleaning %d Thrift generated files in %s".format(filesToDelete.size, Project.displayFull(resolvedScoped)))
      IO.delete(filesToDelete)
    }
  }
}
