package nl.biopet.bioconda

import java.io.{FileNotFoundException, IOException}
import java.util.function.Supplier

import sbt.{File, URL}

import scala.language.postfixOps
import com.roundeights.hasher.Implicits._
import com.roundeights.hasher.ByteReader
import com.typesafe.sbt.git.GitRunner
import ohnosequences.sbt.GithubRelease.keys.TagName
import org.apache.commons.io.FileUtils
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
                   log: ManagedLogger,
                   remotes: Boolean = false
                  ): Boolean = {
    // TODO: Find a git command that just returns branches as a list. (Without * in front of the branch you are on)

    // Without "--no-color" scala doesn't match the strings properly. Color matters in string comparison!
    val branchList: Array[String] = {
      if (remotes) {
        git.apply("branch", "-a", "--no-color")(repo, log).split("\\n")
      }
      else {
        git.apply("branch", "--no-color")(repo, log).split("\\n")
      }
    }
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
    try { test.!!(log)}

    catch {
      case e: Exception => throw new Exception(s"Docker does not run: ${e.getMessage}")
    }
    }
  def circleCiCommand(cwd: File, args: Seq[String], log:ManagedLogger) = {
    val path = cwd.getPath
    val command = Seq("docker",
      "run",
      "--rm",
      "-v", "/var/run/docker.sock:/var/run/docker.sock",
      "-v", s"$path:$path",
      "--workdir", s"$path",
      "circleci/picard",
      "circleci") ++ args
    (Process(command,cwd).lineStream(log)).foreach(line => log.info(line))
    // if (exit != 0) throw new Exception(s"Command ${command.mkString(" ")} failed with exit code: ${exit}.")
  }

  def copyDirectory(source: File, dest: File, permissions: Boolean = true):Unit = {
    assert(source.isDirectory,"Source should be a directory")
    if (dest.exists()) {
      if (!dest.isDirectory) {
        throw new IOException(s"Destination ${dest.getAbsolutePath} is a file, not a directory.")
      }
    } else {
      dest.mkdirs()
    }
    for (file <- source.listFiles()) {
      val destination = new File(dest,file.getName)
      if (file.isDirectory) {
        destination.mkdir()
        copyDirectory(file,destination,permissions = permissions)
      }
      else {
        // Simple file copy is used here for maximum control.
        FileUtils.copyFile(file,destination)
        if (permissions) {
          destination.setReadable(file.canRead)
          destination.setWritable(file.canWrite)
          destination.setExecutable(file.canExecute)
        }
        }
    }
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
