name := """imsforwordnews"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs
)

libraryDependencies +=  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41"
libraryDependencies +=  "org.xerial" % "sqlite-jdbc" % "3.8.0-SNAPSHOT"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
