lazy val root = (project in file(".")).settings(
  name := "openhackbackend",
  organization := "info.ditrapani",
  version := "0.0.1",
  scalaVersion := "2.12.6"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-unchecked",
  "-Xlint",
  "-Ypartial-unification",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused",
  "-Ywarn-value-discard",
  "-Xfuture"
)

val circeVersion = "0.9.3"
val Http4sVersion = "0.18.9"

libraryDependencies ++= Seq(
  "io.fabric8" % "kubernetes-client" % "3.1.11",
  "io.fabric8" % "kubernetes-model" % "2.0.9",
  "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  // test
  "org.mockito" % "mockito-core" % "2.18.3" % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

wartremoverWarnings ++= Warts.allBut(
  Wart.Equals,
  Wart.NonUnitStatements,
  Wart.ToString
)

scalafmtVersion in ThisBuild := "1.5.1"
scalafmtOnCompile in Compile := true
