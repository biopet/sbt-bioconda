package nl.biopet.bioconda


import sbt.{File, URL}

import scala.language.postfixOps
import com.roundeights.hasher.Implicits._
import com.roundeights.hasher.ByteReader
import com.typesafe.sbt.git.GitRunner
import ohnosequences.sbt.GithubRelease.keys.TagName
import org.kohsuke.github.{GHAsset, GHRelease, GHRepository}
import sbt.internal.util.ManagedLogger

import scala.collection.JavaConverters
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.matching.Regex

object BiocondaUtils {
  def getSha256SumFromDownload(url: String): String = {
    val jar = new URL(url)
    jar.openStream().sha256
  }

  def getSourceUrl(tag: TagName, repo: GHRepository): Option[String] = {
    val releaseList = repo.listReleases().asList()
    val releases =
      JavaConverters.collectionAsScalaIterable(releaseList).toList
    val currentRelease = releases.find(x => x.getTagName == tag)
    if (currentRelease.isEmpty) {
      throw new Exception(
        s"'$tag' tag not present on release page. Please release on github before publishing to bioconda.")
    }
    val assets = JavaConverters
      .collectionAsScalaIterable(
        currentRelease.getOrElse(new GHRelease).getAssets)
      .toList
    val releaseJar =
      // Finds all jars. This assumes only one jar is released.
      assets.find(x => x.getBrowserDownloadUrl.contains(".jar"))
    if (releaseJar.isEmpty) { None } else
      Some(releaseJar.getOrElse(new GHAsset).getBrowserDownloadUrl)
  }
  def branchExists(branch: String,
                   repo: File,
                   git: GitRunner,
                   log: ManagedLogger): Boolean = {
    // TODO: Find a git command that just returns branches as a list. (Without * in front of the branch you are on)

    // Without "--no-color" scala doesn't match the strings properly. Color matters in string comparison!
    val branchList: Array[String] =
      git.apply("branch", "-a", "--no-color")(repo, log).split("\\n")

    val branches = new ListBuffer[String]

    // For each branch
    // Remove that annonoying *
    // Split on / and get the last item(remotes/origin/branch) => branch
    // Trim away all spaces
    branchList.foreach(x =>
      branches.append(x.replaceFirst("\\*", "").split("/").last.trim()))
    branches.toList.contains(branch)

  }
  def getVersionFromYaml(metaYaml: File): String = {
    val versionRegex: Regex = "version:.([0-9\\.]+)".r
    val yaml = Source.fromFile(metaYaml).getLines().mkString
    val matches = versionRegex.findAllIn(yaml).matchData
    val version = matches.toList.head.group(1)
    println(version)
    version
  }

}
