name := "currency-monitoring"

version := "0.1"

scalaVersion := "2.12.0"

val circeVersion = "0.11.1"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp" %% "async-http-client-backend-cats" % "1.6.6",
  "com.typesafe" % "config" % "1.3.4",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
)