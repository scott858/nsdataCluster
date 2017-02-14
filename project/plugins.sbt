logLevel := Level.Warn

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.3")
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.8.2")  // for sbt-0.13.x or higher

libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.5.47"
