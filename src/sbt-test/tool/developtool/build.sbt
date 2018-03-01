lazy val root = (project in file(".")).settings(
  name := "DummyTool",
  organizationName := "Dummy Organization",
  organization := "example.dummy",
  startYear := Some(2017),
  biopetUrlName := "dummytool",
  biopetIsTool := true,
  mainClass in assembly := Some(s"nl.biopet.tools.dummytool.DummyTool"),
  scalaVersion := "2.11.11"
  )

libraryDependencies += "com.github.biopet" %% "tool-utils" % "0.2"
libraryDependencies += "com.github.biopet" %% "tool-test-utils" % "0.1" % Test

TaskKey[Unit]("checkValues") := {
  val validGitRepo = "git@github.com:biopet/dummytool.git"
  val validHomePage = Some(url("https://github.com/biopet/dummytool"))

  assert(git.remoteRepo.value == validGitRepo, s"'${git.remoteRepo.value}' does not equal '$validGitRepo'")
  assert(homepage.value == validHomePage, s"'${homepage.value}' does not equal '$validHomePage'")
  assert(useGpg.value, "useGpg should be true")
  assert(biopetIsTool.value, "biopetIsTool should be true")
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
