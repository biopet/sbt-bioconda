package nl.biopet.bioconda

import org.kohsuke.github
import sbt.{Def, _}
import Keys._
import com.typesafe.sbt.GitPlugin
//import ohnosequences.sbt.SbtGithubReleasePlugin.autoImport.
import sbtassembly.AssemblyKeys.{assemblyJarName, assemblyOutputPath,assembly}
import ohnosequences.sbt.GithubRelease.keys.{ghreleaseGetRepo, ghreleaseAssets}
import com.typesafe.sbt.SbtGit.GitKeys
import com.typesafe.sbt.git.GitRunner
import GitKeys.{gitBranch, gitCurrentBranch, gitRemoteRepo}
import org.kohsuke.github.{GHAsset, GHRelease}
import sbt.internal.util.ManagedLogger
import com.roundeights.hasher.Implicits._
import scala.language.postfixOps
import scala.collection.JavaConverters
import scala.collection.mutable.ListBuffer

object BiocondaPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = {
    empty
  } //GitPlugin && SbtGithubReleasePlugin

  object autoImport extends BiocondaKeys

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Def.settings(
    biocondaBranch := normalizedName.value,
    biocondaMainGitUrl := "https://github.com/bioconda/bioconda-recipes.git",
    biocondaMainBranch := "master",
    biocondaUpdatedRepository := initBiocondaRepo.value,
    biocondaUpdatedBranch := updateBranch.dependsOn(biocondaUpdatedRepository).value,
    biocondaRepository := new File(target.value, "bioconda"),
    biocondaRecipeDir := new File(target.value, "conda-recipe"),
    biocondaSourceUrl := getSourceUrl.value,
    biocondaSha256Sum := getSha256Sum.value
  )

  override def globalSettings: Seq[Def.Setting[_]] = Def.settings(
  )


  private def initBiocondaRepo: Def.Initialize[Task[File]] = {
    Def.task {
      val initialized: Boolean = new File(biocondaRepository.value, ".git").exists()
      val git = GitKeys.gitRunner.value
      val s = streams.value
      val local = biocondaRepository.value
      val upstream = biocondaMainGitUrl.value
      val origin = biocondaMainGitUrl.value
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


      if (branchExists(branch, local, git, s.log)) {
        git.apply("checkout", branch)(local, s.log)
      }
      else {
        git.apply("checkout", "-b", branch)(local, s.log)
      }

      // Get latest recipes from main repository.
      git.apply("pull", "upstream", branch)(local, s.log)
      local
    }
  }

  private def updateBranch: Def.Initialize[Task[File]] = {
    Def.task {
      val git = GitKeys.gitRunner.value
      val s = streams.value
      val local: File = biocondaRepository.value
      val branch: String = biocondaBranch.value
      val mainBranch: String = biocondaMainBranch.value

      // Check if tool branch exists, create it otherwise.
      if (branchExists(branch, local, git, s.log)) {
        git.apply("checkout", branch)(local, s.log)
      }
      else {
        git.apply("checkout", "-b", branch)(local, s.log)
      }
      // Rebase tool branch on main branch
      git.apply("rebase", mainBranch)(local, s.log)
      local
    }
  }

  private def getSha256Sum: Def.Initialize[Task[String]] =
    Def.task {
      val jar: sbt.File = assemblyOutputPath.value
      jar.sha256.hex
    }.dependsOn(assembly)
  private def getSourceUrl: Def.Initialize[Task[String]] =
    Def.task {
      val repo = ghreleaseGetRepo.value
      val releaseList = repo.listReleases().asList()
      val releases = JavaConverters.collectionAsScalaIterable(releaseList).toList
      val currentRelease = releases.find(x => x.getTagName() == version.value)
      if (!currentRelease.isDefined) {
        throw new Exception(s"'${version.value}' tag not present on release page. Please release on github before publishing to bioconda.")
      }
      val assets = JavaConverters.collectionAsScalaIterable(currentRelease.getOrElse(new GHRelease).getAssets()).toList
      val releaseJar = assets.find(x => x.getBrowserDownloadUrl.contains(assemblyJarName.value))
      if (!releaseJar.isDefined) {
        throw new Exception (s"'")
      }
      releaseJar.getOrElse(new GHAsset).getBrowserDownloadUrl
      }


  private def createRecipe: Def.Initialize[Task[File]] = {
    Def.task {
      val recipe = new BiocondaRecipe(
        name = (name in bioconda).value,
        version = (version in bioconda).value,
        sourceUrl = biocondaSourceUrl.value,
        sourceSha256 = "",
        runRequirements = biocondaRequirements.value,
        homeUrl = (homepage in bioconda).value.getOrElse("").toString,
        license = licenses.value.toList.last._1,
        buildRequirements = List(),
        summary = "",
        description = "",
        buildNumber = 0,
        notes = ""
      )
      new File("/")
    }
  }

  def branchExists(branch: String,
                   repo: File,
                   git: GitRunner,
                   log: ManagedLogger
                   ): Boolean = {
    // TODO: Find a git command that just returns branches as a list. (Without * in front of the branch you are on)

    // Without "--no-color" scala doesn't match the strings properly. Color matters in string comparison!
    val branchList: Array[String] =
      git.apply("branch", "-a", "--no-color")(repo, log).split("\\n")

    val branches = new ListBuffer[String]

    // For each branch
    // Remove that annonoying *
    // Split on / and get the last item(remotes/origin/branch) => branch
    // Trim away all spaces
    branchList.foreach(x => branches.append(x.replaceFirst("\\*","").split("/").last.trim()))
    branches.toList.contains(branch)

  }
}
