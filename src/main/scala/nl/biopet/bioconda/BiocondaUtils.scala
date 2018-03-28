package nl.biopet.bioconda

import java.io.FileNotFoundException

import sbt.{URL, File}

import scala.language.postfixOps
import com.roundeights.hasher.Implicits._
import com.roundeights.hasher.ByteReader
import com.typesafe.sbt.git.GitRunner
import ohnosequences.sbt.GithubRelease.keys.TagName
import org.eclipse.jgit.errors.CommandFailedException
import org.kohsuke.github.{GHAsset, GHRelease, GHRepository}
import sbt.internal.util.ManagedLogger

import sys.process._
import scala.collection.JavaConverters
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.io.Source
import scala.util.matching
import scala.util.matching.Regex

object BiocondaUtils {
  def getSha256SumFromDownload(url: String): Option[String] = {
    val jar = new URL(url)
    try {
      Some(jar.openStream().sha256.hex)
    } catch {
      case e:java.io.FileNotFoundException => None
    }
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
    if (matches.isEmpty) {
      throw new Exception(s"No version found in: ${metaYaml.getPath}")
    }
    val version = matches.toList.head.group(1)
    version
  }

  def dockerInstalled(log: ManagedLogger, path: Option[String] = None) : Unit = {
    val testCommand = "docker version -f '{{.Client.Version}}'"
    def test: ProcessBuilder = if (path.isDefined) {
      Process(Seq("bash", "-c", testCommand), None, "PATH" -> path.getOrElse("$PATH"))
    }
    else {
      Process(Seq("bash", "-c", testCommand), None)
    }
    try { test.run(log)}

    catch {
      case e: Exception => throw new Exception(s"Docker does not run: ${e.getMessage}")
    }
    }
  def circleCiCommand(cwd: File, args: Seq[String], log:ManagedLogger) = {
    val path = cwd.getPath
    Seq("docker",
      "run",
      "--rm",
      "-v", "/var/run/docker.sock:/var/run/docker.sock",
      "-v", s"$path:$path",
      "--workdir", s"$path",
      "circleci/picard",
      "circleci") ++ args
  }

  def testBioconda(log: ManagedLogger, directory: File): Unit = {
    dockerInstalled(log)
    val test = Process(circleCiCommand(directory,Seq("build"),log=log),cwd = directory)
    test.run(log)
  }

  /**
    * Executes a copy command on the system command line
    * @param source source string
    * @param dest destination string
    * @param recursive set to true for recursive copying.
    */
  def copy(source: String, dest: String, log: ManagedLogger, recursive: Boolean = false):Unit = {
    val r = if (recursive) "-r" else ""
    val copyCommand = s"cp $r $source $dest"
    Process(Seq("bash", "-c",copyCommand)).run(log)
  }

  def crawlRecipe(recipe: File): Seq[File] = {
    val files = recipe.listFiles()
    val yamls = new ArrayBuffer[File]()
    for (file <- files) {
      if (file.isDirectory) {
        yamls ++= crawlRecipe(file)
      }
      if (file.getName == "meta.yaml") {
        yamls.append(file)
      }
    }
    yamls
  }
}
