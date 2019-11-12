enablePlugins(AutomateHeaderPlugin)
enablePlugins(GitVersioning)

name := "sbt-sassy"
organization := "io.bullet"
homepage := Some(new URL("https://github.com/sirthias/sbt-sassy/"))
description := "SBT plugin for Dart SASS"
startYear := Some(2015)
licenses := Seq("Apache 2" -> url("https://www.apache.org/licenses/LICENSE-2.0.html"))
unmanagedResources in Compile += baseDirectory.value.getParentFile.getParentFile / "LICENSE"
scmInfo := Some(ScmInfo(url("https://github.com/sirthias/sbt-sassy/"), "scm:git:git@github.com:sirthias/sbt-sassy.git"))

scalaVersion := "2.12.10"
sbtPlugin := true

git.useGitDescribe := true

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:_",
  "-unchecked",
  "-target:jvm-1.8",
  "-Xlint:_,-missing-interpolator",
  "-Xfatal-warnings",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ybackend-parallelism", "8",
  "-Ywarn-unused:imports,-patvars,-privates,-locals,-implicits,-explicits",
  "-Ycache-macro-class-loader:last-modified",
  "-Yno-adapted-args",
  "-Ywarn-inaccessible",
  "-Ywarn-infer-any",
  "-Ywarn-nullary-override",
  "-Ywarn-nullary-unit",
  "-Xfuture",
  "-Xsource:2.13"
)

scalacOptions in (Compile, doc) += "-no-link-warnings"
sourcesInBase := false

// file headers
headerLicense := Some(HeaderLicense.ALv2("2015-2019", "Jens Grassel, Mathias Doenitz"))

// reformat main and test sources on compile
scalafmtOnCompile := true

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.4")

testFrameworks += new TestFramework("utest.runner.Framework")

libraryDependencies ++= Seq(
  "io.bullet"   %% "borer-core" % "1.1.0",
  "com.lihaoyi" %% "utest"      % "0.7.1" % "test"
)

// publishing
publishMavenStyle := false
publishArtifact in (Compile, packageBin) := true
publishArtifact in (Test, packageBin) := false
publishArtifact in (Compile, packageDoc) := false
publishArtifact in (Compile, packageSrc) := true
bintrayRepository := "sbt-plugins"
bintrayOrganization in bintray := None