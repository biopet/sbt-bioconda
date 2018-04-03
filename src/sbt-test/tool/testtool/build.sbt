lazy val root = (project in file(".")).settings(
  name := "testtool",
  organizationName := "biopet",
  organization := "biopet",
  startYear := Some(2017),
  //mainClass in assembly := Some(s"nl.biopet.tools.dummytool.DummyTool"),
  scalaVersion := "2.11.11",
  biocondaGitUrl := "https://github.com/biopet/bioconda-recipes.git",
  biocondaMainGitUrl := "https://github.com/biopet/bioconda-recipes.git",
  licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
  libraryDependencies += "com.github.biopet" %% "tool-utils" % "0.2",
  ghreleaseRepoOrg := "biopet",
  ghreleaseRepoName := "testtool",
  biocondaRepository := biocondaTempDir
)

// Home directory is used because using /tmp gives errors while testing
def biocondaTempDir: File = {
  val homeTest = new File(sys.env("HOME"))
  val dir = java.io.File.createTempFile("bioconda",".d",homeTest)

  dir.delete()
  dir.mkdirs()
  dir.deleteOnExit()
  dir
}




