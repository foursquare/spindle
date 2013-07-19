seq(Default.scalate: _*)

// resourceGenerators in Compile <+= (baseDirectory) map { (in) => 
//   val sspSources: Seq[File] = ((in ** "*.ssp").get)

// }

// sourceGenerators in Compile <+= (fullClasspath in (codegen, Runtime), runner in codegen, baseDirectory, sourceManaged, streams) map { (cp, r, in, out, s) =>
//   val template = file("src/main/ssp/codegen/scala/record.ssp")
//   val extension = "scala"
//   val namespaceOut = out / "scala_record"
//   namespaceOut.mkdirs()
//   val thriftSources: Seq[File] = ((in ** "*.thrift").get)
//   r.run(
//     "com.foursquare.recordv2.codegen.ThriftCodegen",
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
