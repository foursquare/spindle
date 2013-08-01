seq(Default.scala: _*)

libraryDependencies ++= (
  ThirdParty.jackson ++
  ThirdParty.thrift ++
  ThirdParty.scalajCollection ++
  ThirdParty.finagleThrift)

sourceGenerators in Compile <+= (fullClasspath in (codegenBinary, Runtime), runner in codegenBinary, baseDirectory, sourceManaged, streams) map { (cp, r, in, out, s) =>
  val template = file("src/main/ssp/codegen/scala/record.ssp")
  val extension = "scala"
  out.mkdirs()
  val thriftSources: Seq[File] = ((in ** "*.thrift").get)
  r.run(
    "com.foursquare.spindle.codegen.binary.ThriftCodegen",
    cp.files,
    Seq(
      "--template", template.absolutePath,
      "--extension", extension,
      "--namespace_out", out.absolutePath) ++
    thriftSources.map(_.absolutePath),
    s.log
  ).foreach(error)
  (out ** "*.scala") get
}
