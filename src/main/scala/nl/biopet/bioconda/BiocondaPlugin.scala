package nl.biopet.bioconda

import sbt.{Def, _}
import com.typesafe.sbt.GitPlugin
import ohnosequences.sbt.SbtGithubReleasePlugin
import com.typesafe.sbt.SbtGit.GitKeys
import com.typesafe.sbt.git.GitRunner
import GitKeys.{gitBranch,gitRemoteRepo}
object BiocondaPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = noTrigger
  override def requires: Plugins = GitPlugin && SbtGithubReleasePlugin

  object autoImport extends BiocondaKeys
  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = super.projectSettings

  override def globalSettings: Seq[Def.Setting[_]] = super.globalSettings
}
