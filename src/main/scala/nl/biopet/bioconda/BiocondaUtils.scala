/*
 * Copyright (c) 2018 Sequencing Analysis Support Core - Leiden University Medical Center
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.biopet.bioconda

import com.typesafe.sbt.git.GitRunner
import ohnosequences.sbt.GithubRelease.keys.TagName
import org.kohsuke.github.{GHRelease, GHRepository, GitHub}
import sbt.internal.util.ManagedLogger
import sbt.{File, URL}
import nl.biopet.utils.conversions.{yamlFileToMap,any2map}

import scala.collection.JavaConverters
import scala.io.Source
import scala.sys.process._
import scala.util.matching.Regex

object BiocondaUtils {
  def getSourceUrl(tag: TagName, repo: GHRepository): URL = {
    val repoName = repo.getName
    val repoOwner = repo.getOwnerName
    // Disable authentication. These jars should be accessible for non authenticated users.
    val noAuthRepo =
      GitHub.connectAnonymously().getUser(repoOwner).getRepository(repoName)
    val releaseList = noAuthRepo.listReleases().asList()
    val releases =
      JavaConverters.collectionAsScalaIterable(releaseList).toList
    val currentRelease = releases.find(x => x.getTagName == tag)
    if (currentRelease.isEmpty) {
      throw new java.io.FileNotFoundException(
        s"'$tag' tag not present on release page. Please release on github before publishing to bioconda.")
    }
    val assets = JavaConverters
      .collectionAsScalaIterable(
        currentRelease.getOrElse(new GHRelease).getAssets)
      .toList
    val releaseJar =
      // Finds all jars. This assumes only one jar is released.
      assets.find(
        x =>
          x.getBrowserDownloadUrl.endsWith(".jar") &&
            !x.getBrowserDownloadUrl.endsWith("javadoc.jar") &&
            !x.getBrowserDownloadUrl.endsWith("sources.jar")
      )
    releaseJar match {
      case Some(x) => new URL(x.getBrowserDownloadUrl)
      case _ =>
        throw new java.io.FileNotFoundException(
          s"No valid release jar found for: $tag")
    }
  }
  def branchExists(branch: String,
                   repo: File,
                   git: GitRunner,
                   log: ManagedLogger,
                   remotes: Boolean = false): Boolean = {
    // TODO: Find a git command that just returns branches as a list. (Without * in front of the branch you are on)

    // Without "--no-color" scala doesn't match the strings properly. Color matters in string comparison!
    val branchList: Array[String] = {
      if (remotes) {
        git("branch", "-a", "--no-color")(repo, log).split("\\n")
      } else {
        git("branch", "--no-color")(repo, log).split("\\n")
      }
    }
    val branches: Array[String] = branchList.flatMap(x => {
      val branchInfo: Array[String] = { x.replaceFirst("\\*", "").split("/") }
      if (branchInfo.isEmpty) None
      else Some(branchInfo.lastOption.getOrElse("").trim())
    })
    branches.contains(branch)

  }
  def getVersionFromYaml(metaYaml: File): String = {
    val packageValue: Map[String, Any] =
      any2map(
        yamlFileToMap(metaYaml)
          .getOrElse("package", Map()))
    val version = packageValue.getOrElse("version", throw new Exception(s"No version found in ${metaYaml.getPath}"))
    version.toString
  }

  def dockerInstalled(log: ManagedLogger, path: Option[String] = None): Unit = {
    val testCommand = "docker version -f '{{.Client.Version}}'"
    def test: ProcessBuilder =
      if (path.isDefined) {
        Process(Seq("bash", "-c", testCommand),
                None,
                "PATH" -> path.getOrElse("$PATH"))
      } else {
        Process(Seq("bash", "-c", testCommand), None)
      }
    try { test.!!(log) } catch {
      case e: RuntimeException =>
        throw new RuntimeException(s"Docker does not run: ${e.getMessage}")
    }
  }
  def circleCiCommand(cwd: File,
                      args: Seq[String],
                      log: ManagedLogger): Unit = {
    val path = cwd.getPath
    val command = Seq("docker",
                      "run",
                      "--rm",
                      "-v",
                      "/var/run/docker.sock:/var/run/docker.sock",
                      "-v",
                      s"$path:$path",
                      "--workdir",
                      s"$path",
                      "circleci/picard",
                      "circleci") ++ args
    Process(command, cwd).lineStream(log).foreach(line => log.info(line))
    // if (exit != 0) throw new Exception(s"Command ${command.mkString(" ")} failed with exit code: ${exit}.")
  }

  def pullLatestUtils(log: ManagedLogger): Unit = {
    Process(Seq("docker", "pull", "bioconda/bioconda-utils-build-env"))
      .lineStream(log)
      .foreach(line => log.info(line))
  }

  class GitUrlParser(gitUrl: String) {
    assert(gitUrl.startsWith("http") || gitUrl.startsWith("git"))
    val gitUrlParts: Array[String] = gitUrl.split(":/".toCharArray).reverse
    def repo: String = gitUrlParts(0).stripSuffix(".git")
    def owner: String = gitUrlParts(1)
  }
}
