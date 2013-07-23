seq(Default.scala: _*)

assemblySettings

addArtifact(Artifact("thrift-codegen-binary", "assembly"), AssemblyKeys.assembly)

libraryDependencies ++= (
  ThirdParty.argot ++
  ThirdParty.scalate ++
  ThirdParty.slf4jNoLogging)

libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.9.1"
