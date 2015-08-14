seq(Default.scala: _*)


libraryDependencies <++= (scalaVersion)(v => {
  ThirdParty.argot(v) ++
  ThirdParty.scalate(v) ++
  ThirdParty.slf4jNoLogging
})

libraryDependencies <+= (scalaVersion)(v => "org.scala-lang" % "scala-compiler" % v)
