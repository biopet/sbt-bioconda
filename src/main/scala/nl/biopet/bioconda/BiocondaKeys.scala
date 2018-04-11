/*
 * Copyright (c) 2018 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.biopet.bioconda
import sbt._

trait BiocondaKeys {
  lazy val Bioconda = config("bioconda") describedAs "Configuration for bioconda repo"
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
  lazy val biocondaPushRecipes =
    taskKey[Unit]("Push the branch with the recipe.")
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
    taskKey[String]("The summary describing the program")
  lazy val biocondaNotes = taskKey[String]("Usage notes for the program")
  lazy val biocondaDefaultJavaOptions = settingKey[Seq[String]](
    "The default java options for the program when started with the wrapperscript.")
  lazy val biocondaTestCommands = settingKey[Seq[String]](
    "The commands that are used to test whether the program was succesfuly installed with conda.")
  lazy val biocondaLicense =
    settingKey[String]("The license displayed in the bioconda recipe.")
  lazy val biocondaAddRecipes =
    taskKey[File]("Adds generated recipes to bioconda recipe and commits them")
  lazy val biocondaCommitMessage = taskKey[String](
    "The commit message with which new recipes will be submitted")
  lazy val biocondaTestRecipes =
    taskKey[File]("Tests the generated recipes with circleci")
  lazy val biocondaOverwriteRecipes = settingKey[Boolean](
    "Whether recipes already published on bioconda main should be overwritten")
  lazy val biocondaCommand = settingKey[String](
    "The command that is used when the tool is installed in the environment")
  lazy val biocondaPullRequest =
    taskKey[Unit]("Create a pull request on bioconda main")
  lazy val biocondaPullRequestTitle =
    taskKey[String]("The title of the pull request")
  lazy val biocondaPullRequestBody =
    taskKey[String]("The message accompanying the pull request")
  lazy val biocondaRelease = taskKey[Unit](
    "Create recipes, test them, and create a pull request on bioconda main.")
  lazy val biocondaIsReleased = settingKey[Boolean]("Whether the tool is released on bioconda already")
  lazy val biocondaSkipErrors = settingKey[Boolean](
    "Recipes with failing elements are skipped instead of causing an exception.")
}
