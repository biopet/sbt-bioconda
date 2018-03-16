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
    biocondaRepository := new File(target.value, "bioconda")
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

      if (!initialized) {
        git.apply("init")(local, s.log)
      }
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

      val branches: Array[String] =
        git.apply("branch")(local, s.log).split("\\n")
      if (branches.contains(branch)) {
        git.apply("checkout",branch)(local, s.log)
      }
      else{
        git.apply("checkout","-b",branch)(local, s.log)
      }
      git.apply("pull","upstream",branch)(local,s.log)
      local
    }
  }
}
