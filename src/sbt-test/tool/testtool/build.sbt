import org.kohsuke.github.{GHRepository, GitHub}
import org.apache.commons.io.FileUtils
import sbt.Keys.resolvers
import sbt._
lazy val checkRepo = taskKey[Unit]("checks if repo is checked out")
lazy val checkRecipes = taskKey[Unit]("checks if recipes are created")
lazy val checkCopy = taskKey[Unit]("checks if recipes are copied")
lazy val deleteTmp = taskKey[Unit]("Deletes temporary test dir")
lazy val checkTexts = taskKey[Unit]("Checks whether the texts have been initialized correctly")
name := "testtool"
organizationName := "biopet"
organization := "biopet"
startYear := Some(2017)
homepage := Some(new URL("https://github.com/biopet/testtool"))
//mainClass in assembly := Some(s"nl.biopet.tools.dummytool.DummyTool")
scalaVersion := "2.11.11"
biocondaGitUrl := "https://github.com/biopet/bioconda-recipes.git"
licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))
libraryDependencies += "com.github.biopet" %% "tool-utils" % "0.2"
libraryDependencies += "org.kohsuke" % "github-api" % "1.92"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5"
ghreleaseRepoOrg := "biopet"
ghreleaseRepoName := "testtool"
ghreleaseGetRepo := getRepo.value
biocondaRepository := biocondaTempDir.value
checkRepo := Def.task {
filesExistInDir(biocondaRepository.value,
      Seq(".github",
        "recipes",
        "scripts",
        "recipes/biopet",
        "config.yml")
    )
  }.value
  checkRecipes := Def.task {
    filesExistInDir(biocondaRecipeDir.value,
      Seq("0.1/meta.yaml",
        "0.1/build.sh",
        "0.1/testtool.py",
        "meta.yaml",
        "build.sh",
        "testtool.py")
    )
  }.value
  checkCopy := Def.task {
    filesExistInDir(biocondaRepository.value,
      Seq("recipes/testtool/0.1/meta.yaml",
        "recipes/testtool/0.1/build.sh",
        "recipes/testtool/0.1/testtool.py",
        "recipes/testtool/meta.yaml",
        "recipes/testtool/build.sh",
        "recipes/testtool/testtool.py")
    )
  }.value
  deleteTmp := Def.task{FileUtils.deleteDirectory(biocondaRepository.value)}.value
    resolvers += Resolver.sonatypeRepo("snapshots")

def fileExistsInDir(dir: File, file: String): Unit = {
  assert(new File(dir,file).exists(), s"$file should exist in $dir")
}
def filesExistInDir(dir:File, files: Seq[String]): Unit = {
  files.foreach(file => fileExistsInDir(dir, file))
}
def getRepo: Def.Initialize[Task[GHRepository]] = Def.task {
  val github = GitHub.connectAnonymously()
  val repo = s"${ghreleaseRepoOrg.value}/${ghreleaseRepoName.value}"
  github.getRepository(repo)
}
// Home directory is used because using /tmp gives errors while testing
def biocondaTempDir: Def.Initialize[File] = {
  Def.setting {
  val homeTest = new File(sys.env("HOME"))
  val dir = java.io.File.createTempFile("bioconda",".d",homeTest)
  dir.delete()
  dir.mkdirs()
  dir.deleteOnExit()
  dir
}}

def textChecking: Def.Initialize[Task[Unit]] = {
  Def.task {
    val summary: String = biocondaSummary.value
    val notes: String = biocondaNotes.value
    val pullRequestBody: String = biocondaPullRequestBody.value
    val pullRequestTitle: String = biocondaPullRequestTitle.value
  }
}
