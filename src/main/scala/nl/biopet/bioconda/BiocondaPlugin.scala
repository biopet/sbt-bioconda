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
    biocondaUpdatedRepository := updatedRepo(biocondaRepository,
      biocondaGitUrl,
      biocondaBranch,
      biocondaMainGitUrl,
      biocondaMainBranch).value,
    biocondaRepository := new File(target.value , "bioconda")
  )

  override def globalSettings: Seq[Def.Setting[_]] = Def.settings(
  )

  /*
   * Copied from https://github.com/sbt/sbt-ghpages/blob/master/src/main/scala/com/typesafe/sbt/sbtghpages/GhpagesPlugin.scala
   */
  private def updatedRepo(
      repo: SettingKey[File],
      remote: SettingKey[String],
      branch: SettingKey[String],
      upstream: SettingKey[String],
      upstreamBranch: SettingKey[String]): Def.Initialize[Task[File]] =
    Def.task[File] {
      val local = repo.value
      val git = GitKeys.gitRunner.value
      val s = streams.value

      // Make sure there is a git repo checked out at the desired branch
      git.updated(remote = remote.value,
                  cwd = local,
                  branch = Some(upstreamBranch.value),
                  log = s.log)

      // Make sure the upstream git url is added
      val remotes: Array[String] =
        git.apply("remote")(local, streams.value.log).split("\\n")
      if (remotes.contains("upstream")) {
        git.apply("remote", "set-url", "upstream", upstream.value)(local, s.log)
      } else {
        git.apply("remote", "add", "upstream", upstream.value)(local, s.log)
      }

      // rebase the current branch on upstream main, so a pull request can be made
      // from this branch.
      git.apply("pull", "upstream", upstreamBranch.value)(local, s.log)
      // git.apply("rebase", "upstream", upstreamBranch.value)(local, s.log)
      local
    }
}
