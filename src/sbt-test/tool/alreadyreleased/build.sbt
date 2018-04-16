import org.kohsuke.github.{GHRepository, GitHub}
import org.apache.commons.io.FileUtils
import sbt.Keys.resolvers
import sbt._
lazy val checkRepo = taskKey[Unit]("checks if repo is checked out")
lazy val checkRecipes = taskKey[Unit]("checks if recipes are created")
lazy val checkCopy = taskKey[Unit]("checks if recipes are copied")
lazy val deleteTmp = taskKey[Unit]("Deletes temporary test dir")
lazy val checkTexts = taskKey[Unit]("Checks whether the texts have been initialized correctly")
name := "biopet"
organizationName := "biopet"
organization := "biopet"
startYear := Some(2014)
homepage := Some(new URL("https://github.com/biopet/biopet"))

scalaVersion := "2.11.11"
biocondaGitUrl := "https://github.com/biopet/bioconda-recipes.git"
licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))
checkTexts := textChecking.value
deleteTmp := Def.task{FileUtils.deleteDirectory(biocondaRepository.value)}.value

ghreleaseRepoOrg := "biopet"
ghreleaseRepoName := "biopet"
biocondaRepository := biocondaTempDir.value

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
    val newTool: Boolean = biocondaNewTool.value
    val commitMessage: String = biocondaCommitMessage.value
    assert(!newTool, "This tool should already be published")
    assert(summary.contains("This summary for biopet is automatically generated"))
    assert(pullRequestBody.contains("[ ] This PR adds a new recipe."), "Pull request template should be included")
    assert(pullRequestTitle.contains("New version for biopet"),"Pull request title should mention new version")
    assert(commitMessage.contains("Added new versions for biopet"))
  }
}
