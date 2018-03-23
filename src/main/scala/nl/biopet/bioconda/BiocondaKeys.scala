package nl.biopet.bioconda

import ohnosequences.sbt.GithubRelease.keys.TagName
import sbt._

trait BiocondaKeys {
  lazy val Bioconda = config("bioconda") describedAs ("Configuration for bioconda repo")
  lazy val biocondaMainGitUrl = settingKey[String]("Git URL of biocondaMain")
  lazy val biocondaMainBranch =
    settingKey[String]("The default development branch on bioconda main.")
  lazy val biocondaGitUrl =
    settingKey[String]("Git URL of your/your project's bioconda repo fork")
  lazy val biocondaBranch =
    settingKey[String]("Branch for bioconda tool repository")
  lazy val biocondaUpdatedRepository =
    taskKey[File](
      "Make sure the repo is up to date with the main branch of the main bioconda-recipes repo.")
  lazy val biocondaUpdatedBranch =
    taskKey[File]("Update the tool branch to the main bioconda-recipes repo")
  lazy val biocondaRecipeDir =
    settingKey[File]("Where the recipes will be created")
  lazy val biocondaPushRecipe = taskKey[File](
    "Copy the recipe in the bioconda repo, commit it and push the branch.")
  lazy val biocondaCreateRecipes = taskKey[File](
    "Create the bioconda recipes for all released versions of the tool and the latest(default) version.")
  lazy val biocondaCreateLatestRecipe = taskKey[File](
    "Create the bioconda recipe for the latest released version of the tool")
  lazy val biocondaCreateVersionRecipes = taskKey[File](
    "Create the bioconda recipes for all released versions of the tool.")
  lazy val biocondaRepository =
    settingKey[File](
      "Sandbox environment where the bioconda git branch is checked out.")
  lazy val biocondaRequirements =
    settingKey[Seq[String]]("A list of requirements for the tool")
  lazy val biocondaBuildRequirements =
    settingKey[Seq[String]]("A list of build requirements for the tool")
  lazy val biocondaBuildNumber = settingKey[Int]("The build number")
  lazy val biocondaSummary =
    settingKey[String]("The summary describing the program")
  lazy val biocondaNotes = settingKey[String]("Usage notes for the program")
  lazy val biocondaDefaultJavaOptions = settingKey[Seq[String]](
    "The default java options for the program when started with the wrapperscript.")
  lazy val biocondaTestCommands = settingKey[Seq[String]](
    "The commands that are used to test whether the program was succesfuly installed with conda.")
  lazy val biocondaLicense =
    settingKey[String]("The license displayed in the bioconda recipe.")
}
