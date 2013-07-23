package com.foursquare.recordv2.codegen.plugin

import sbt._
import sbt.Fork.ForkJava
import sbt.Keys.TaskStreams
import java.io.File

object ThriftCodegenPlugin extends Plugin {
  val Thrift   = config("thrift").hide

  val thrift = TaskKey[Seq[File]]("thrift", "Generate Scala sources from Thrift files(s)")
  val thriftCodegenVersion = SettingKey[String]("thrift-codegen-version", "Version of thrift-codegen-binary to use.")

  val thriftSettings     = Seq[Project.Setting[_]](
    Keys.ivyConfigurations += Thrift,
    Keys.libraryDependencies <+= (thriftCodegenVersion)(v =>
      "com.foursquare.common" %% "thrift-codegen-binary" % v % "thrift"
    )
  ) ++ thriftSettingsIn(Compile) ++ thriftSettingsIn(Test)

  def thriftSettingsIn(conf: Configuration): Seq[Project.Setting[_]] =
    inConfig(conf)(thriftSettings0) ++ Seq(Keys.clean <<= Keys.clean.dependsOn(Keys.clean in thrift in conf))

  private def thriftSettings0 = Seq[Project.Setting[_]](
    Keys.sources in thrift       <<= Keys.unmanagedResourceDirectories.map(dirs => (dirs ** "*.thrift").get),
    Keys.sourceManaged in thrift ~= (_ / "thrift"), // e.g. /target/scala-2.8.1.final/src_managed/main/thrift
    thrift                       <<= (Keys.javaHome, Keys.classpathTypes in thrift, Keys.update, Keys.sources in thrift,
                                  Keys.sourceManaged in thrift, Keys.resolvedScoped, Keys.streams).map(thriftCompile),
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
      sourceManaged: File,
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
      val appOptions = Seq(
        "--template", "src/main/ssp/codegen/scala/record.ssp",
        "--extension", "scala",
        "--namespace_out", sourceManaged.absolutePath
      )
      val mainClass  = "com.foursquare.recordv2.codegen.binary.ThriftCodegen"

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
