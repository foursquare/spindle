seq(Default.test: _*)

libraryDependencies ++= ThirdParty.phonenumbers.map(_ % "test")

libraryDependencies ++= ThirdParty.slf4jNoLogging.map(_ % "test")

libraryDependencies <++= (scalaVersion)(v => ThirdParty.finagleThrift(v))

