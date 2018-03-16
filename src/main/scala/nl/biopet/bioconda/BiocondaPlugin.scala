package nl.biopet.bioconda

import sbt.{Def, _}
import Keys._
import com.typesafe.sbt.GitPlugin
import ohnosequences.sbt.SbtGithubReleasePlugin
import com.typesafe.sbt.SbtGit.GitKeys
import com.typesafe.sbt.git.GitRunner
import GitKeys.{gitBranch, gitRemoteRepo}

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
    biocondaRecipeDir := new File(target.value, "conda-recipe")
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


      if (branchExists(branch)){
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
        if (branchExists(branch) {
          git.apply("checkout", branch)(local, s.log)
        }
        else {
          git.apply("checkout", "-b", branch)(local, s.log)
        }
        // Rebase tool branch on main branch
        git.apply("rebase", mainBranch)(local,s.log)
        local
      }
    }

  def branchExists(branch: String): Boolean = {
    val git = GitKeys.gitRunner.value
    val s = streams.value
    val local: File = biocondaRepository.value

    // TODO: Find a git command that just returns branches as a list. (Without * in front of the branch you are on)
    val branches: Array[String] =
      git.apply("branch", "-a")(local, s.log).split("\\n")

    // For each branch
    // Split on / and get the last item(remotes/origin/branch) => branch
    // Remove that annonoying *
    // Trim away all spaces
    // Is this the branch we are looking for?
    branches.filter(x => x.split("/").last.replaceFirst("\\*"," ").trim == branch).contains(true)
  }
}
