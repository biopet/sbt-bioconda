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
import nl.biopet.utils.conversions.{any2map, yamlFileToMap}
import ohnosequences.sbt.GithubRelease.keys.TagName
import org.kohsuke.github.{GHRelease, GHRepository}
import sbt.internal.util.ManagedLogger
import sbt.{File, URL}

import scala.collection.JavaConverters
import scala.sys.process._

object BiocondaUtils {
  /**
    * Get a url for the released jar on github.
    * @param tag The tag for the release
    * @param repo The github repo
    * @return a URL from which the jar can be downloaded.
    */
  def getSourceUrl(tag: TagName, repo: GHRepository): URL = {
    val releaseList = repo.listReleases().asList()
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

  /**
    * Checks if a branch exists
    * @param branch the branch name
    * @param repo directory where the git repository is
    * @param git the GitRunner
    * @param log the log
    * @param remotes if remotes should be checked if the branch exists
    * @return true if the branch exists
    */
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

  /**
    * Get a version from a meta.yaml file for conda
    * @param metaYaml the meta.yaml file
    * @return the version as a string.
    */
  def getVersionFromYaml(metaYaml: File): String = {
    val packageValue: Map[String, Any] =
      any2map(
        yamlFileToMap(metaYaml)
          .getOrElse("package", Map()))
    val version = packageValue.getOrElse(
      "version",
      throw new Exception(s"No version found in ${metaYaml.getPath}"))
    version.toString
  }

  /**
    * Check if docker is installed.
    * @param log the log file to which the version message is issued.
    * @param environment Set extra environment variables. Useful if PATH needs to be changed
    */
  def dockerInstalled(log: ManagedLogger,
                      environment: Map[String, String] = Map()): Unit = {
    val testCommand = "docker version -f '{{.Client.Version}}'"
    def test: ProcessBuilder =
      Process(Seq("bash", "-c", testCommand), None, environment.toList: _*)
    try { test.!!(log) } catch {
      case e: RuntimeException =>
        throw new RuntimeException(s"Docker does not run: ${e.getMessage}")
    }
  }

  /**
    * Run circle ci in a directory
    * @param cwd the directory in which circleci should run.
    * @param args the arguments with which circleci should be run
    * @param log the log to which the circleci output will be written.
    */
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
  }

  /**
    * Pull the latest bioconda-utils-build-env image.
    * @param log
    */
  def pullLatestUtils(log: ManagedLogger): Unit = {
    Process(Seq("docker", "pull", "bioconda/bioconda-utils-build-env"))
      .lineStream(log)
      .foreach(line => log.info(line))
  }

  /**
    * A parser that gets the repo and owner from a git url.
    * @param gitUrl the git url.
    */
  class GitUrlParser(gitUrl: String) {
    assert(gitUrl.startsWith("http") || gitUrl.startsWith("git"))
    val gitUrlParts: Array[String] = gitUrl.split(":/".toCharArray).reverse
    def repo: String = gitUrlParts(0).stripSuffix(".git")
    def owner: String = gitUrlParts(1)
  }
}
