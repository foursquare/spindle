seq(Default.thrift: _*)

// libraryDependencies ++= (
//   ThirdParty.jackson ++
//   ThirdParty.scalajCollection ++
//   ThirdParty.thrift
// )

// sourceGenerators in Compile <+= (/*externalDependencyClasspath in Runtime, runner in codegenBinary,*/ baseDirectory, sourceManaged, streams) map { (/*cp, r,*/ in, out, s) =>
//   val template = file("src/main/ssp/codegen/scala/record.ssp")
//   val extension = "scala"
//   val namespaceOut = out / "scala_record"
//   namespaceOut.mkdirs()
//   val thriftSources: Seq[File] = ((in ** "*.thrift").get)
//   com.foursquare.recordv2.codegen.binary.ThriftCodegen.main(
//     Array(
//       "--template", template.absolutePath,
//       "--extension", extension,
//       "--namespace_out", namespaceOut.absolutePath) ++
//     thriftSources.map(_.absolutePath))
//   (out ** "*.scala") get
// }

// sourceGenerators in Compile <+= (fullClasspath in (codegenBinary, Runtime), runner in codegenBinary, baseDirectory, sourceManaged, streams) map { (cp, r, in, out, s) =>
//   val template = file("src/main/ssp/codegen/scala/record.ssp")
//   val extension = "scala"
//   val namespaceOut = out / "scala_record"
//   namespaceOut.mkdirs()
//   val thriftSources: Seq[File] = ((in ** "*.thrift").get)
//   r.run(
//     "com.foursquare.recordv2.codegen.binary.ThriftCodegen",
//     cp.files,
//     Seq(
//       "--template", template.absolutePath,
//       "--extension", extension,
//       "--namespace_out", namespaceOut.absolutePath) ++
//     thriftSources.map(_.absolutePath),
//     s.log
//   ).foreach(error)
//   (out ** "*.scala") get
// }
