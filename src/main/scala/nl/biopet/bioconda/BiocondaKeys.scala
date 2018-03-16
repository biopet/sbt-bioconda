package nl.biopet.bioconda

import sbt._

trait BiocondaKeys {
  lazy val bioconda = config("bioconda") describedAs ("Configuration for bioconda repo")
  lazy val biocondaMainGitUrl = settingKey[String]("Git URL of biocondaMain")
  lazy val biocondaMainBranch =
    settingKey[String]("The default development branch on bioconda main.")
  lazy val biocondaGitUrl =
    settingKey[String]("Git URL of your/your project's bioconda repo fork")
  lazy val biocondaBranch =
    settingKey[String]("Branch for bioconda tool repository")
  lazy val biocondaUpdatedRepository =
    taskKey[File]("Make sure the repo is up to date with the main branch of the main bioconda-recipes repo.")
  lazy val biocondaUpdatedBranch = taskKey[File]("Update the tool branch to the main bioconda-recipes repo")
  lazy val biocondaRecipeDir = settingKey[File]("Where the recipes will be created")
  lazy val biocondaPushRecipe = taskKey[File]("Copy the recipe in the bioconda repo, commit it and push the branch.")
  lazy val biocondaRepository =
    settingKey[File](
      "Sandbox environment where the bioconda git branch is checked out.")
}
