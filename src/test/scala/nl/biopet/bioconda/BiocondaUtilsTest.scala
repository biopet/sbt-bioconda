package nl.biopet.bioconda

import java.io.{File, PrintWriter}

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import sbt.Keys.streams
import BiocondaUtils._
import ohnosequences.sbt.GithubRelease.keys.TagName
import org.kohsuke.github.{GHAsset, GHRelease, GHRepository, GitHub}
import sbt.internal.util.{BufferedLogger, ConsoleLogger, ManagedLogger}
import org.apache.logging.log4j.core.LoggerContext
import org.scalatest.exceptions.TestFailedException

import scala.io.Source

class BiocondaUtilsTest extends TestNGSuite with Matchers {
  val s4j = new LoggerContext("test").getLogger("test")
  val log = new ManagedLogger("test", None, None, s4j)
  @Test
  def testSha256Sum(): Unit = {
    // Taken the README from Biopet 0.9.0. Small, link should be stable
    val downloadLink: String =
      "https://raw.githubusercontent.com/biopet/biopet/be7838f27f3cad9f80191d92a4a795c34d1ae092/README.md"
    getSha256SumFromDownload(downloadLink) shouldBe Some("186e801bf3cacbd564b4ec00815352218038728bd6787b71f65db474a3588901")
    getSha256SumFromDownload(downloadLink + "nonsense") shouldBe None
  }
  @Test
  def testGetSourceUrl(): Unit = {
    val github: GitHub = GitHub.connect()
    // Use old biopet repo to get test data. Should be stable.
    val repository: GHRepository =
      github.getOrganization("biopet").getRepository("biopet")
    val link: Option[String] = Some(
      "https://github.com/biopet/biopet/releases/download/v0.9.0/Biopet-0.9.0-be7838f2.jar")
    getSourceUrl(tag = "v0.9.0", repository) shouldBe (link)
  }

  @Test
  def testGetVersionFromYaml(): Unit = {
    val yaml = Source.fromResource("nl/biopet/bioconda/meta.yaml")
    val tmp = File.createTempFile("meta", ".yaml")
    val writer = new PrintWriter(tmp)
    val string = yaml.mkString
    writer.write(string)
    writer.close()
    getVersionFromYaml(tmp) shouldBe ("0.9.0")
  }

  @Test
  def testDockerNotInstalled(): Unit = {
    intercept[Exception] {
      dockerInstalled(log,path=Some(""))}.getMessage() shouldBe "Docker does not run: Nonzero exit value: 127"
  }

  @Test
  def testDockerInstalled(): Unit = {
    dockerInstalled(log)
  }

  @Test
  def testCircleciCommand(): Unit = {
    circleCiCommand(new File(""),Seq("version"),log)

  }
}
