organization := "com.github.biopet"
name := "sbt-bioconda"

homepage := Some(url(s"https://github.com/biopet/sbt-bioconda"))
licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))
organizationName := "Sequencing Analysis Support Core - Leiden University Medical Center"
scmInfo := Some(
  ScmInfo(
    url("https://github.com/biopet/sbt-bioconda"),
    "scm:git@github.com:biopet/sbt-bioconda.git"
  )
)
startYear := some(2018)

developers := List(
  Developer(id = "rhpvorderman",
            name = "Ruben Vorderman",
            email = "r.h.p.vorderman@lumc.nl",
            url = url("https://github.com/rhpvorderman"))
)

publishMavenStyle := true

sbtPlugin := true

scalaVersion := "2.12.5"

useGpg := true

scalafmt := (scalafmt in Compile)
  .dependsOn(scalafmt in Test)
  .dependsOn(scalafmt in Sbt)
  .value

headerCreate := (headerCreate in Compile).dependsOn(headerCreate in Test).value
headerCheck := (headerCheck in Compile).dependsOn(headerCheck in Test).value
publishTo := {
  if (isSnapshot.value)
    Some(Opts.resolver.sonatypeSnapshots)
  else
    Some(Opts.resolver.sonatypeStaging)
}

scriptedLaunchOpts := {
  scriptedLaunchOpts.value ++
    Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
}
scriptedBufferLog := false

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  releaseStepCommand("git fetch"),
  releaseStepCommand("git checkout master"),
  releaseStepCommand("git pull"),
  releaseStepCommand("git merge origin/develop"),
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  //releaseStepCommand("ghpagesPushSite"),
  releaseStepCommand("publishSigned"),
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges,
  releaseStepCommand("git checkout develop"),
  releaseStepCommand("git merge master"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
libraryDependencies += "com.github.biopet" %% "common-utils" % "0.6"
libraryDependencies += "org.testng" % "testng" % "6.14.2" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies += "org.kohsuke" % "github-api" % "1.92"

libraryDependencies ++= Seq(
  Defaults.sbtPluginExtra(
    "ohnosequences" % "sbt-github-release" % "0.7.0",
    (sbtBinaryVersion in pluginCrossBuild).value,
    (scalaBinaryVersion in pluginCrossBuild).value
  ),
  Defaults.sbtPluginExtra(
    "com.typesafe.sbt" % "sbt-git" % "0.9.3",
    (sbtBinaryVersion in pluginCrossBuild).value,
    (scalaBinaryVersion in pluginCrossBuild).value
  )
)
