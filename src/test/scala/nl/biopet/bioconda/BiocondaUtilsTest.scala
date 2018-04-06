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

import java.io.{File, PrintWriter}
import java.net.URL

import nl.biopet.bioconda.BiocondaUtils._
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.core.{Logger, LoggerContext}
import org.kohsuke.github.{GHRepository, GitHub}
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import sbt.internal.util.ManagedLogger

import scala.io.Source

class BiocondaUtilsTest extends TestNGSuite with Matchers {
  val s4j: Logger = new LoggerContext("test").getLogger("test")
  val log: ManagedLogger = new ManagedLogger("test", None, None, s4j)

  @Test
  def testGetSourceUrl(): Unit = {
    if (sys.env.getOrElse("TRAVIS", "false") != "true") {
      val github: GitHub = GitHub.connectAnonymously()
      // Use old biopet repo to get test data. Should be stable.
      val repository: GHRepository =
        github.getOrganization("biopet").getRepository("biopet")
      val link: URL = new URL(
        "https://github.com/biopet/biopet/releases/download/v0.9.0/Biopet-0.9.0-be7838f2.jar")
      getSourceUrl(tag = "v0.9.0", repository) shouldBe link
      intercept[java.io.FileNotFoundException] {
        getSourceUrl(tag = "v3.3.3", repository)
      }.getMessage shouldBe s"'v3.3.3' tag not present on release page. Please release on github before publishing to bioconda."
    }
  }

  @Test
  def testGetVersionFromYaml(): Unit = {
    val yaml = Source.fromResource("nl/biopet/bioconda/meta.yaml")
    val tmp = File.createTempFile("meta", ".yaml")
    tmp.deleteOnExit()
    val writer = new PrintWriter(tmp)
    val string = yaml.mkString
    writer.write(string)
    writer.close()
    getVersionFromYaml(tmp) shouldBe "0.9.0"
  }

  @Test
  def testDockerNotInstalled(): Unit = {
    intercept[Exception] {
      dockerInstalled(log, path = Some(""))
    }.getMessage shouldBe "Docker does not run: Nonzero exit value: 127"
  }

  @Test
  def testDockerInstalled(): Unit = {
    dockerInstalled(log)
  }

  @Test
  def testCircleciCommand(): Unit = {
    val tmp = File.createTempFile("docker", "test")
    tmp.delete()
    tmp.mkdir()
    circleCiCommand(tmp, Seq("version"), log)
    FileUtils.deleteDirectory(tmp)

  }
  @Test
  def testCircleCiCommandFail(): Unit = {
    val tmp = File.createTempFile("docker", "test")
    tmp.delete()
    tmp.mkdir()
    intercept[Exception] {
      circleCiCommand(tmp, Seq("bladnajsdnk"), log)
    }.getMessage should include("Nonzero exit code: 1")
    FileUtils.deleteDirectory(tmp)
  }

  @Test
  def testGitUrlParser(): Unit = {
    new GitUrlParser("git@github.com:bioconda/bioconda-recipes.git").owner shouldBe "bioconda"
    new GitUrlParser("git@github.com:bioconda/bioconda-recipes.git").repo shouldBe "bioconda-recipes"
    new GitUrlParser("https://github.com/bioconda/bioconda-recipes.git").owner shouldBe "bioconda"
    new GitUrlParser("https://github.com/bioconda/bioconda-recipes.git").repo shouldBe "bioconda-recipes"
  }

  @Test
  def testPullLatestUtils(): Unit = {
    pullLatestUtils(log)
  }
}
