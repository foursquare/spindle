seq(Default.scala: _*)

libraryDependencies <++= (scalaVersion)(v => {
  ThirdParty.commonsIo ++
  ThirdParty.parboiledScala(v) ++
  ThirdParty.finagleThrift(v)
})
