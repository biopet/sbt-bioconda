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

import com.typesafe.sbt.GitPlugin
import com.typesafe.sbt.SbtGit.GitKeys
import nl.biopet.bioconda.BiocondaTexts._
import nl.biopet.bioconda.BiocondaUtils._
import nl.biopet.utils.io.{copyDir, getSha256SumFromDownload, listDirectory}
import ohnosequences.sbt.GithubRelease.keys.{TagName, ghreleaseGetRepo}
import ohnosequences.sbt.SbtGithubReleasePlugin
import org.kohsuke.github.GitHub
import sbt.Keys._
import sbt.{Def, _}

import scala.collection.JavaConverters

object BiocondaPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = {
    GitPlugin && SbtGithubReleasePlugin
  }

  object autoImport extends BiocondaKeys

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Def.settings(
    biocondaBranch := (normalizedName in Bioconda).value,
    name in Bioconda := normalizedName.value,
    biocondaCommand := (name in Bioconda).value,
    biocondaMainGitUrl := "https://github.com/bioconda/bioconda-recipes.git",
    biocondaMainBranch := "master",
    biocondaUpdatedRepository := initBiocondaRepo.value,
    biocondaUpdatedBranch := updateBranch().value,
    biocondaRepository := new File(target.value, "bioconda"),
    biocondaRecipeDir := new File(target.value, "recipes"),
    biocondaBuildNumber := 0,
    biocondaRequirements := Seq("openjdk"),
    biocondaBuildRequirements := Seq(),
    biocondaNotes := defaultNotes.value,
    biocondaSummary := defaultSummary.value,
    biocondaDefaultJavaOptions := Seq(),
    biocondaCreateRecipe := createCurrentRecipe().value,
    biocondaCreateAllRecipes := createAllRecipes().value,
    biocondaLicense := getLicense.value,
    biocondaTestCommands := Seq(),
    biocondaCommitMessage := defaultCommitMessage.value,
    biocondaAddRecipes := addRecipes().value,
    biocondaTestRecipes := testRecipes.value,
    biocondaOverwriteRecipes := false,
    biocondaPushRecipes := pushRecipe.value,
    biocondaPullRequestBody := defaultPullRequestBody.value,
    biocondaPullRequestTitle := defaultPullRequestTitle.value,
    biocondaPullRequest := createPullRequest.value,
    biocondaRelease := releaseProcedure().value,
    biocondaSkipErrors := false,
    biocondaNewTool := isNewTool.value
  )

  /**
    * The release procedure for this specific version
    * @return a task that depends on all the tasks necessary for releasing this version
    */
  def releaseProcedure(): Def.Initialize[Task[Unit]] =
    Def
      .task {}
      .dependsOn(biocondaPullRequest)
      .dependsOn(biocondaPushRecipes)
      .dependsOn(biocondaTestRecipes)
      .dependsOn(biocondaAddRecipes)
      .dependsOn(biocondaCreateRecipe)

  /**
    * The release procedure for all versions released on github
    * @return a task that depends on all the tasks necessary for releasing all versions
    */
  def releaseAllProcedure(): Def.Initialize[Task[Unit]] =
    Def
      .task {}
      .dependsOn(biocondaPullRequest)
      .dependsOn(biocondaPushRecipes)
      .dependsOn(biocondaTestRecipes)
      .dependsOn(biocondaAddRecipes)
      .dependsOn(biocondaCreateAllRecipes)

  /**
    * Creates a new local bioconda git repo
    * @return a file that is the directory of the bioconda repo
    */
  private def initBiocondaRepo: Def.Initialize[Task[File]] = {
    Def.task {
      val initialized: Boolean =
        new File(biocondaRepository.value, ".git").exists()
      val git = GitKeys.gitRunner.value
      val s = streams.value
      val local = biocondaRepository.value
      val upstream = biocondaMainGitUrl.value
      val origin = biocondaGitUrl.value
      val branch = biocondaMainBranch.value

      //Check if git repo already exits
      if (!initialized) {
        git("init")(local, s.log)
      }

      // Check if remotes have been set properly
      val remotes: Array[String] =
        git("remote")(local, s.log).split("\\n")

      def addRemote(remote: String, url: String): Unit = {
        if (remotes.contains(remote)) {
          val currentRemoteUrl = git("remote", "get-url", remote)(local, s.log)
          if (currentRemoteUrl == url) {} else {
            throw new Exception(
              s"""Git repository already has url: '$currentRemoteUrl' defined for remote
                 |'$remote'. Will not set '$remote' to '$url'.""".stripMargin)
          }
        } else {
          git("remote", "add", remote, url)(local, s.log)
        }
      }

      addRemote("upstream", upstream)
      addRemote("origin", origin)

      // Check if biocondaMainBranch exists. If not create it.

      if (branchExists(branch, local, git, s.log, remotes = true)) {
        git("checkout", branch)(local, s.log)
      } else {
        git("checkout", "-b", branch)(local, s.log)
      }

      // Get latest recipes from main repository.
      git("pull", "upstream", branch)(local, s.log)
      local
    }
  }

  /**
    * Creates a new branch in the bioconda repo that is up to date with upstream
    * @return the bioconda repo with the branch checked out
    */
  private def updateBranch(): Def.Initialize[Task[File]] = {
    Def
      .task {
        val git = GitKeys.gitRunner.value
        val s = streams.value
        val local: File = biocondaRepository.value
        val branch: String = biocondaBranch.value
        val mainBranch: String = biocondaMainBranch.value

        // Check if tool branch exists, delete it if so.
        // This will allow for the recreation of failed recipes that
        // are not yet in bioconda main.
        if (branchExists(branch, local, git, s.log)) {
          git("branch", "-D", branch)(local, s.log)
        }
        git("checkout", "-b", branch)(local, s.log)
        // Rebase tool branch on main branch
        git("rebase", mainBranch)(local, s.log)
        local
      }
      .dependsOn(biocondaUpdatedRepository)
  }

  /**
    * Gets the released tags from github and determines the latest
    * @return the latest to be released tag.
    */
  private def getLatestTag: Def.Initialize[Task[TagName]] = {
    Def.task {
      getReleasedTags.value
        .sortBy(tag => tag.stripPrefix("v"))
        .lastOption
        .getOrElse("No version")
    }
  }

  /**
    * Create the recipe for the current version
    * @return the directory with the recipes
    */
  private def createCurrentRecipe(): Def.Initialize[Task[File]] = {
    Def.task {
      val tag = "v" + version.value
      val releasedTags = getReleasedTags.value
      if (!releasedTags.contains(tag)) {
        throw new Exception(
          s"Please release tag '$tag' with the githubRelease plugin first.")
      }
      val publishedTags = getPublishedTags.value
      if (publishedTags.contains(tag) && !biocondaOverwriteRecipes.value) {
        throw new Exception(s"""Tag '$tag' is already released.
             |Please set 'biocondaOverwriteRecipes' to 'true'
             |if you want to overwrite the recipe.
             |""".stripMargin.replace("\n", " "))
      }
      createRecipes(Seq(tag)).value
    }
  }

  /**
    * Creates the recipes for all versions on github
    * @return the directory containing the recipes.
    */
  private def createAllRecipes(): Def.Initialize[Task[File]] = {
    Def.task {
      val publishedTags: Seq[TagName] = getPublishedTags.value
      val releasedTags: Seq[TagName] = getReleasedTags.value
      val toBePublishedTags: Seq[TagName] =
        if (biocondaOverwriteRecipes.value) releasedTags
        else releasedTags.filter(tag => !publishedTags.contains(tag))
      createRecipes(toBePublishedTags).value
    }
  }

  /**
    * creates the recipes for tags.
    * Also creates the default recipe for the latest version if the latest version
    * is among the tags.
    * @param tags The tags for which a recipe needs to be created
    * @return the directory with the recipe.
    */
  private def createRecipes(tags: Seq[TagName]): Def.Initialize[Task[File]] = {
    Def
      .task {
        val repo = ghreleaseGetRepo.value
        val log = streams.value.log

        val summary = biocondaSummary.value
        val notes = biocondaNotes.value

        val latest = getLatestTag.value
        for (tag <- tags) {
          try {
            val sourceUrl: URL = getSourceUrl(tag, repo)
            log.info(s"Downloading ${sourceUrl.toString} to get sha256sum.")
            val sourceSha256: String = getSha256SumFromDownload(sourceUrl)
            log.info("Downloading complete.")
            val versionNumber
              : String = tag.stripPrefix("v") //hardcoded "v" here. ugly.
            val homeUrl = (homepage in Bioconda).value
              .map(_.toString)
              .getOrElse(throw new IllegalArgumentException(
                "Please define (homepage in Bioconda). Required."))
            val recipe = new BiocondaRecipe(
              name = (name in Bioconda).value,
              version = versionNumber,
              command = biocondaCommand.value,
              sourceUrl = sourceUrl.toString,
              sourceSha256 = sourceSha256,
              runRequirements = biocondaRequirements.value,
              homeUrl = homeUrl,
              license = biocondaLicense.value,
              buildRequirements = biocondaBuildRequirements.value,
              summary = summary,
              buildNumber = biocondaBuildNumber.value,
              notes = Some(notes),
              defaultJavaOptions = biocondaDefaultJavaOptions.value,
              testCommands = biocondaTestCommands.value
            )

            val publishDir = new File(biocondaRecipeDir.value, versionNumber)
            publishDir.mkdirs()
            recipe.createRecipeFiles(publishDir)
            // If tag is latest, create the default recipe for this tag.
            if (tag == latest) {
              recipe.createRecipeFiles(biocondaRecipeDir.value)
            }
          } catch {
            case e: java.io.FileNotFoundException =>
              if (biocondaSkipErrors.value) {
                log.error(
                  s"Error while preparing recipe: ${e.getMessage}. Skipping recipe")
              }
              // other errors are unexpected. Fail here.
              else throw e
          }
        }
        biocondaRecipeDir.value
      }
  }

  /**
    * Determines which tags are already published in bioconda.
    * @return a sequence of tagnames
    */
  private def getPublishedTags: Def.Initialize[Task[Seq[TagName]]] = {
    Def.taskDyn {
      Def
        .task {
          val biocondaRecipes: File =
              new File(biocondaRepository.value, "recipes")
            val toolRecipes =
              new File(biocondaRecipes, (name in Bioconda).value)
            if (toolRecipes.exists()) {
              val yamlFiles = listDirectory(toolRecipes,
                                            Some("^meta.ya?ml$".r),
                                            recursive = true)
              // Hardcoded "v" prefix here. Is the standard in github release plugin.
              // But not a very nice way of doing it.
              yamlFiles.map(x => "v" + getVersionFromYaml(x))
            } else Seq()
          }
        }
        .dependsOn(biocondaUpdatedBranch)
    }

  /**
    * Determines wheter the tool is new by checking
    * if the tool name already exists in the main repo.
    * @return true if tool is not yet present in main repo
    */
  private def isNewTool: Def.Initialize[Task[Boolean]] = {
    Def.task {
      val biocondaRecipes: File =
        new File(biocondaRepository.value, "recipes")
      val toolRecipes =
        new File(biocondaRecipes, (name in Bioconda).value)
      !toolRecipes.exists()
    }.dependsOn(biocondaUpdatedBranch)
  }

  /**
    * Determines which tags are released on github.
    * @return a sequence of tagnames.
    */
  private def getReleasedTags: Def.Initialize[Task[Seq[TagName]]] = {
    Def.task {
      val log = streams.value.log
      val repo = ghreleaseGetRepo.value
      val releaseList = repo.listReleases().asList()
      val releases =
        JavaConverters.collectionAsScalaIterable(releaseList).toList
      val tags = releases.flatMap(release => {
        val tag: TagName = release.getTagName
        try {
          getSourceUrl(tag, repo)
          Some(tag)
        } catch {
          case e: java.io.FileNotFoundException =>
            if (biocondaSkipErrors.value) {
              log.error(s"Release $tag does not have a jar attached. Skipping")
              None
            } else throw e
        }
      })
      if (tags.isEmpty) {
        throw new Exception(
          "No tags have been released. Please release on github before publishing to bioconda")
      }
      tags
    }
  }

  /**
    * Add the recipes directory to the bioconda git repo and commit.
    * @return the git repo with the new commit
    */
  private def addRecipes(): Def.Initialize[Task[File]] = {
    Def.task {
      val log = streams.value.log
      val repo: File = biocondaUpdatedBranch.value
      val recipes: File = biocondaRecipeDir.value
      val git = GitKeys.gitRunner.value
      val message = biocondaCommitMessage.value
      val biocondaRecipes = new File(repo, "recipes")
      val toolRecipes = new File(biocondaRecipes, (name in Bioconda).value)
      copyDir(recipes, toolRecipes)
      git("add", ".")(repo, log)
      git("commit", "-m", message)(repo, log)
      repo
    }
  }

  /**
    * Start a docker container with a circle ci image to test the recipes.
    * @return the repo with the now tested recipes.
    */
  private def testRecipes: Def.Initialize[Task[File]] =
    Def.task {
      val repo = biocondaRepository.value
      val log = streams.value.log
      dockerInstalled(log)
      pullLatestUtils(log)
      circleCiCommand(repo, Seq("build"), log)
      repo
    }

  /**
    * Force push the repo branch to the biocondaRepository
    */
  private def pushRecipe: Def.Initialize[Task[Unit]] =
    Def.task {
      val log = streams.value.log
      val repo = biocondaRepository.value
      val git = GitKeys.gitRunner.value
      // Force the push. This automated branch should prevail.
      git("push", "-f", "origin", biocondaBranch.value)(repo, log)
    }

  /**
    * Create a pull request on the bioconda main repo.
    * @return A task that creates the pull request.
    */
  private def createPullRequest: Def.Initialize[Task[Unit]] =
    Def.task {
      val biocondaMain = new GitUrlParser(biocondaMainGitUrl.value)
      val biocondaOwn = new GitUrlParser(biocondaGitUrl.value)
      val github = GitHub.connect()
      val biocondaMainRepo =
        github.getRepository(s"${biocondaMain.owner}/${biocondaMain.repo}")

      // title of pull request
      val title = biocondaPullRequestTitle.value

      // branch that should be merged in "owner:branch" format
      val head = s"${biocondaOwn.owner}:${biocondaBranch.value}"

      // branch that we merge into
      val base = biocondaMainBranch.value

      // Text accompanying the pull request
      val body = biocondaPullRequestBody.value

      biocondaMainRepo.createPullRequest(title, head, base, body)
    }

  /**
    * Gets the license from your project by taking the name from the head of the license list.
    * @return A license as a string.
    */
  private def getLicense: Def.Initialize[String] = {
    Def.setting {
      (licenses in Bioconda).value.headOption match {
        case Some((string, _)) => string
        case _                 => "No license"
      }
    }
  }
}
