name := """voteforpizza"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

organization := "com.voteforpizza"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "org.postgresql" % "postgresql" % "9.3-1103-jdbc41",
  "com.voteforpizza" %% "singletransferablevote" % "1.0"
)
