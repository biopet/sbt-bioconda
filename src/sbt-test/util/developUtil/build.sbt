lazy val root = (project in file(".")).settings(
  name := "DummyUtil",
  organizationName := "Dummy Organization",
  organization := "example.dummy",
  startYear := Some(2018),
  biopetUrlName := "dummy-util",
  biopetIsTool := false,
  scalaVersion := "2.11.11"
)
libraryDependencies += "log4j" % "log4j" % "1.2.17"
libraryDependencies += "commons-io" % "commons-io" % "2.1"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.3"
libraryDependencies += "org.yaml" % "snakeyaml" % "1.17"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.25"
libraryDependencies += "com.github.biopet" %% "test-utils" % "0.2" % Test


TaskKey[Unit]("checkValues") := {
  val validGitRepo = "git@github.com:biopet/dummy-util.git"
  val validHomePage = Some(url("https://github.com/biopet/dummy-util"))

  assert(git.remoteRepo.value == validGitRepo, s"'${git.remoteRepo.value}' does not equal '$validGitRepo'")
  assert(homepage.value == validHomePage, s"'${homepage.value}' does not equal '$validHomePage'")
  assert(useGpg.value, "useGpg should be true")
  assert(biopetIsTool.value != true, "biopetIsTool should be false")
  assert(publishMavenStyle.value, "publishMavenStyle should be true")
  assert(resolvers.value.contains(Resolver.sonatypeRepo("snapshots")), "'snapshots' not present in 'resolvers'")
  assert(resolvers.value.contains(Resolver.sonatypeRepo("releases")), "'releases' not present in 'resolvers'")
  assert(publishTo.value ==
    Def.setting {
      if (isSnapshot.value)
        Some(Opts.resolver.sonatypeSnapshots)
      else
        Some(Opts.resolver.sonatypeStaging)
    }.value, "publishTo has incorrect value")
}
