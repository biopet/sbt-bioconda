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
    biocondaMainRepo := "https://github.com/bioconda/bioconda-recipes.git",
    biocondaMainBranch := "master"
  )
  GitKeys.
  /* Copied from https://github.com/sbt/sbt-ghpages/blob/master/src/main/scala/com/typesafe/sbt/sbtghpages/GhpagesPlugin.scala
   *
   */
  private def updatedRepo(
      repo: SettingKey[File],
      remote: SettingKey[String],
      branch: SettingKey[Option[String]]): Def.Initialize[Task[Unit]] =
    Def.task {
      val local = repo.value
      val git = GitKeys.gitRunner.value
      val s = streams.value
      git.updated(remote = remote.value,
                  cwd = local,
                  branch = branch.value,
                  log = s.log)
    }

}
