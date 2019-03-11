name := "sbt-sassy"
organization := "io.bullet.sbt"
version := "0.5.1"
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

sbtPlugin := true

scalaVersion := "2.12.8"
scalacOptions ++= Seq(
  "-unchecked",
  "-Xlint",
  "-deprecation",
  "-Xfatal-warnings",
  "-feature",
  "-encoding", "UTF-8"
)

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.4")

libraryDependencies ++= Seq(
  "io.spray"            %%  "spray-json"    % "1.3.5",
  "org.scalatest"       %%  "scalatest"     % "3.0.6" % "test"
)

// Publishing options
// ==================
bintrayOrganization := Option("bullet")
bintrayPackageLabels := Seq("sbt", "sbt-plugin", "sass")
bintrayReleaseOnPublish in ThisBuild := false
bintrayRepository := "sbt-plugins"
publishMavenStyle := false

publish := (publish dependsOn (test in Test)).value

