Seq(Default.thrift: _*)

thriftCodegenIncludes in Compile += file("src/test/thrift")

libraryDependencies ++= ThirdParty.rogueIndex
