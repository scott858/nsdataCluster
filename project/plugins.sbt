logLevel := Level.Warn

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.3")

libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.5.47"
