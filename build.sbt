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
            url = url("https://github.com/rhpvorderman")),
  Developer(id = "ffinfo",
            name = "Peter van 't Hof",
            email = "pjrvanthof@gmail.com",
            url = url("https://github.com/ffinfo"))
)

publishMavenStyle := true

sbtPlugin := true

scalaVersion := "2.12.4"

resolvers += Resolver.sonatypeRepo("snapshots")

useGpg := true

scalafmt := (scalafmt in Compile)
  .dependsOn(scalafmt in Test)
  .dependsOn(scalafmt in Sbt)
  .value

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
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.7"
libraryDependencies += "org.testng" % "testng" % "6.14.2" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies += "org.kohsuke" % "github-api" % "1.92"
libraryDependencies += "org.yaml" % "snakeyaml" % "1.20"
libraryDependencies ++= Seq("com.roundeights" %% "hasher" % "1.2.0")
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
