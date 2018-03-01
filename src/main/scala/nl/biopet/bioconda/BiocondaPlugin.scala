package nl.biopet.bioconda

import sbt._
import com.typesafe.sbt.GitPlugin
import ohnosequences.sbt.SbtGithubReleasePlugin
object BiocondaPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = noTrigger
  override def requires: Plugins = GitPlugin && SbtGithubReleasePlugin

  object autoImport extends BiocondaKeys
  import autoImport._
}
