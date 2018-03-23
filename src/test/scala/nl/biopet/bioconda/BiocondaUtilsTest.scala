package nl.biopet.bioconda

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import BiocondaUtils._

class BiocondaUtilsTest extends TestNGSuite with Matchers {

  @Test
  def testSha256Sum(): Unit = {
    // Taken the README from Biopet 0.9.0. Small, link should be stable
    val downloadLink: String = "https://raw.githubusercontent.com/biopet/biopet/be7838f27f3cad9f80191d92a4a795c34d1ae092/README.md"
    getSha256SumFromDownload(downloadLink) shouldBe("186e801bf3cacbd564b4ec00815352218038728bd6787b71f65db474a3588901")
  }
}
