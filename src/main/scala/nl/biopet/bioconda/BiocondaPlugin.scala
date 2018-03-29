package nl.biopet.bioconda

import com.typesafe.sbt.GitPlugin
import com.typesafe.sbt.SbtGit.GitKeys
import nl.biopet.bioconda.BiocondaUtils._
import ohnosequences.sbt.GithubRelease.keys.{TagName, ghreleaseGetRepo}
import ohnosequences.sbt.SbtGithubReleasePlugin
import org.apache.commons.io.FileUtils
import sbt.Keys._
import sbt.{Def, _}

import scala.collection.JavaConverters
import scala.collection.mutable.ArrayBuffer

object BiocondaPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = {
    GitPlugin && SbtGithubReleasePlugin
  }

  object autoImport extends BiocondaKeys

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Def.settings(
    biocondaBranch := (normalizedName in Bioconda).value,
    name in Bioconda := (normalizedName.value),
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
    biocondaCreateVersionRecipes := createRecipes(versions = true,
                                                  latest = false).value,
    biocondaCreateLatestRecipe := createRecipes(versions = false, latest = true).value,
    biocondaCreateRecipes := createRecipes().value,
    biocondaLicense := (licenses in Bioconda).value.toList.headOption
      .getOrElse("No license", "")
      ._1,
    biocondaTestCommands := Seq(),
    biocondaCommitMessage := s"Automated update for recipes of ${(name in Bioconda).value}",
    biocondaAddRecipes := addRecipes.value,
    biocondaTestRecipes := testRecipes.value,
    biocondaOverwriteRecipes := false,
    biocondaPushRecipes := pushRecipe.value
  )

  override def globalSettings: Seq[Def.Setting[_]] = Def.settings(
    )

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
        git.apply("init")(local, s.log)
      }

      // Check if remotes have been set properly
      val remotes: Array[String] =
        git.apply("remote")(local, s.log).split("\\n")

      if (remotes.contains("upstream")) {
        git.apply("remote", "set-url", "upstream", upstream)(local, s.log)
      } else {
        git.apply("remote", "add", "upstream", upstream)(local, s.log)
      }
      if (remotes.contains("origin")) {
        git.apply("remote", "set-url", "origin", origin)(local, s.log)
      } else {
        git.apply("remote", "add", "origin", origin)(local, s.log)
      }

      // Check if biocondaMainBranch exists. If not create it.

      if (branchExists(branch, local, git, s.log, remotes=true)) {
        git.apply("checkout", branch)(local, s.log)
      } else {
        git.apply("checkout", "-b", branch)(local, s.log)
      }

      // Get latest recipes from main repository.
      git.apply("pull", "upstream", branch)(local, s.log)
      local
    }
  }

  private def updateBranch(): Def.Initialize[Task[File]] = {
    Def.task {
      val git = GitKeys.gitRunner.value
      val s = streams.value
      val local: File = biocondaRepository.value
      val branch: String = biocondaBranch.value
      val mainBranch: String = biocondaMainBranch.value

      // Check if tool branch exists, delete it if so.
      // This will allow for the recreation of failed recipes that
      // are not yet in bioconda main.
      if (branchExists(branch, local, git, s.log)) {
        git.apply("branch","-D", branch)(local, s.log)
      }
      git.apply("checkout", "-b", branch)(local, s.log)
      // Rebase tool branch on main branch
      git.apply("rebase", mainBranch)(local, s.log)
      local
    }.dependsOn(biocondaUpdatedRepository)
  }

  private def createRecipes(
      latest: Boolean = true,
      versions: Boolean = true): Def.Initialize[Task[File]] = {
    Def
      .task {
        val publishedTags: Seq[TagName] = getPublishedTags.value
        val releasedTags: Seq[TagName] = getReleasedTags.value
        // Tags that are released but not in bioconda yet should be published

        val latestTag = releasedTags
          .sortBy(tag => tag.stripPrefix("v"))
          .lastOption
          .getOrElse("No version")

        // make sure latest tag is always published if latest.
        val toBePublishedTags: Seq[TagName] =
          (releasedTags.filter(tag => !publishedTags.contains(tag)) ++
            (if (latest) Seq(latestTag) else Seq())).distinct
        val repo = ghreleaseGetRepo.value
        val log = streams.value.log
        // Some sbt magic here. We initialize a task that returns the recipe dir.
        // We add dependencies to this task based on the tags
        for (tag <- toBePublishedTags) {

          // Only evaluate if versions need to be published.
          if (versions || (latest && tag == latestTag)) {

            // Hardcoded "v".
            val versionNumber = tag.stripPrefix("v")
            val publishDir = new File(biocondaRecipeDir.value, versionNumber)
            val sourceUrl = {
              getSourceUrl(tag, repo)
            }
            if (sourceUrl.isEmpty) {
              log.error(s"No released jar for tag: $tag. Skipping.")
            } else {
              log.info(
                s"Downloading jar from ${sourceUrl.get} to generate checksum.")
              val sourceSha256 = getSha256SumFromDownload(sourceUrl.get)
              if (sourceSha256.isEmpty) {
                log.error(s"Downloading of ${sourceUrl.get} failed. Skipping.")
              } else {
                log.info(s"Downloading finished.")

                val recipe = new BiocondaRecipe(
                  name = (name in Bioconda).value,
                  version = versionNumber,
                  command = biocondaCommand.value,
                  sourceUrl = sourceUrl.getOrElse("No valid source url was found."),
                  sourceSha256 = sourceSha256.getOrElse("No valid checksum was generated."),
                  runRequirements = biocondaRequirements.value,
                  homeUrl = (homepage in Bioconda).value.getOrElse("No homepage was given.").toString,
                  license = biocondaLicense.value,
                  buildRequirements = biocondaBuildRequirements.value,
                  summary = biocondaSummary.value,
                  buildNumber = biocondaBuildNumber.value,
                  notes = Some(biocondaNotes.value),
                  defaultJavaOptions = biocondaDefaultJavaOptions.value,
                  testCommands = biocondaTestCommands.value
                )
                publishDir.mkdirs()
                recipe.createRecipeFiles(publishDir)
                if (tag == latestTag) {
                  recipe.createRecipeFiles(biocondaRecipeDir.value)

                }
              }
            }
          }
        }
        biocondaRecipeDir.value
      }
      .dependsOn(biocondaUpdatedBranch)
  }

  private def defaultSummary: Def.Initialize[String] =
    Def.setting {
      s"""This summary for ${(name in Bioconda).value} is automatically generated.
         |Please visit ${(homepage in Bioconda).value.getOrElse("the project's website")}
         |for more information about this program.
       """.stripMargin.replace("\n"," ")
    }

  private def defaultNotes: Def.Initialize[String] =
    Def.setting {
      def javaOpts: String = {
        val javaDefaults = biocondaDefaultJavaOptions.value
        val builder = new StringBuilder
        javaDefaults.foreach(x => builder.append(x + " "))
        val opts = builder.toString().trim
        if (javaDefaults.isEmpty) "no default java option"
        else opts
      }
      s"""${(name in Bioconda).value} is Java program that comes with a custom wrapper shell script.
         |By default “$javaOpts” is set in the wrapper.
         |The command that runs the program is "${biocondaCommand.value}."
         |If you want to overwrite it you can specify memory options directly after your binaries.
         |If you have _JAVA_OPTIONS set globally this will take precedence.
         |For example run it with “${biocondaCommand.value} -Xms512m -Xmx1g”.
       """.stripMargin.replace("\n"," ")
    }

  private def getPublishedTags: Def.Initialize[Task[Seq[TagName]]] = {
    Def.taskDyn {
      // If recipes are overwritten, they are for the purposes of this plugin not published.
      if (biocondaOverwriteRecipes.value) {
        Def.task {
          val emptyList: Seq[TagName] = Seq()
          emptyList
        }
      }
      else {
        Def
          .task {
            val recipes: File = new File(new File(biocondaRepository.value, "recipes"), (name in Bioconda).value)
            val thisRecipe: File = new File(recipes, (name in Bioconda).value)

            def tags = new ArrayBuffer[String]

            if (thisRecipe.exists()) {
              val metaYamls = crawlRecipe(thisRecipe)
              // Hardcoded "v" prefix here. Is the standard in github release plugin.
              // But not a very nice way of doing it.
              metaYamls.foreach(x => tags.append("v" + getVersionFromYaml(x)))
            }
            tags.toSeq.distinct
          }
          .dependsOn(biocondaUpdatedBranch)
      }
    }
  }

  private def getReleasedTags: Def.Initialize[Task[Seq[TagName]]] = {
    Def.task {
      val log = streams.value.log
      val repo = ghreleaseGetRepo.value
      val releaseList = repo.listReleases().asList()
      val releases =
        JavaConverters.collectionAsScalaIterable(releaseList).toList
      val tags = new ArrayBuffer[TagName]()
      for (release <- releases) {
        val tag = release.getTagName
        val jar = getSourceUrl(tag, repo)
        if (jar.isEmpty) {
          log.info(s"Release $tag does not have a jar attached. Skipping")
        } else tags.append(tag)
      }
      if (tags.isEmpty) {
        throw new Exception(
          "No tags have been released. Please release on github before publishing to bioconda")
      }
      tags
    }
  }

  private def addRecipes: Def.Initialize[Task[File]] = {
    Def.task {
      val log = streams.value.log
      val repo: File = biocondaUpdatedBranch.value
      val recipes: File = biocondaRecipeDir.value
      val git = GitKeys.gitRunner.value
      val message = biocondaCommitMessage.value
      val biocondaRecipes = new File(new File(repo, "recipes"),(name in Bioconda).value)
      copyDirectory(recipes, biocondaRecipes,permissions=true)
      git.apply("add", ".")(repo,log)
      git.apply("commit", "-m", message)(repo,log)
      repo
    }
  }
  private def testRecipes: Def.Initialize[Task[File]] =
    Def.task {
      val repo = biocondaRepository.value
      val log = streams.value.log
      dockerInstalled(log)
      circleCiCommand(repo,Seq("build"),log)
      repo
  }

  private def pushRecipe: Def.Initialize[Task[Unit]] =
    Def.task {
      val log = streams.value.log
      val repo = biocondaRepository.value
      val git = GitKeys.gitRunner.value
      val message = git.apply("push", "origin", biocondaBranch.value)(repo,log)
    }
}
