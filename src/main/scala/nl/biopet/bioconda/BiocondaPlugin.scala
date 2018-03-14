package nl.biopet.bioconda

import sbt.{Def, _}
import Keys._
import com.typesafe.sbt.GitPlugin
import ohnosequences.sbt.SbtGithubReleasePlugin
import com.typesafe.sbt.SbtGit.GitKeys
import com.typesafe.sbt.git.GitRunner
import GitKeys.{gitBranch, gitRemoteRepo}

object BiocondaPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = noTrigger
  override def requires: Plugins = GitPlugin && SbtGithubReleasePlugin

  object autoImport extends BiocondaKeys
  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = Def.settings(
    biocondaBranch := normalizedName.value
  )
  override def globalSettings: Seq[Def.Setting[_]] = Def.settings(
    biocondaMainGitUrl := "https://github.com/bioconda/bioconda-recipes.git",
    biocondaMainBranch := "master",
    biocondaUpdatedRepository := updatedRepo(biocondaRepository,
                                             biocondaGitUrl,
                                             biocondaBranch).value
  )

  /*
   * Copied from https://github.com/sbt/sbt-ghpages/blob/master/src/main/scala/com/typesafe/sbt/sbtghpages/GhpagesPlugin.scala
   */
  private def updatedRepo(
      repo: SettingKey[File],
      remote: SettingKey[String],
      branch: SettingKey[String]): Def.Initialize[Task[File]] =
    Def.task[File] {
      val local = repo.value
      val git = GitKeys.gitRunner.value
      val s = streams.value
      git.updated(remote = remote.value,
        cwd = local,
        branch = Some(branch.value),
        log = s.log)
      local
    }
  private def setUpstream(repo: SettingKey[File],
                          upstream: SettingKey[String]
                               ): Def.Initialize[Task[File]] =
    Def.task[File] {
      val git = GitKeys.gitRunner.value
      val local = repo.value
      val remotes: Array[String] = git.apply("remote")(local,streams.value.log).split("\\n")
      if (remotes.contains("upstream")) {
        git.apply("remote", "set-url","upstream",upstream.value)
      }
      else {
        git.apply("remote", "add", "upstream",upstream.value)
      }
      local
    }
}
