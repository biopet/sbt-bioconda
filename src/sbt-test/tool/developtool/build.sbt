lazy val root = (project in file(".")).settings(
  name := "DummyTool",
  organizationName := "Dummy Organization",
  organization := "example.dummy",
  startYear := Some(2017),
  //mainClass in assembly := Some(s"nl.biopet.tools.dummytool.DummyTool"),
  scalaVersion := "2.11.11",
  biocondaGitUrl := "git@github.com:biopet/bioconda-recipes.git",
  biocondaMainGitUrl := "git@github.com:biopet/bioconda-recipes.git",
  licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))
  )

libraryDependencies += "com.github.biopet" %% "tool-utils" % "0.2"


