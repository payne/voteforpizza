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
  "com.voteforpizza" %% "singletransferablevote" % "1.0"
)
