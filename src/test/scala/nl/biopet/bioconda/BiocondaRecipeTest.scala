package nl.biopet.bioconda

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations
import org.testng.annotations.Test
class BiocondaRecipeTest extends TestNGSuite with Matchers {

  val testRecipe = new BiocondaRecipe(name = "test",
    version = "1.0",
    sourceUrl = "test.example.com/test.jar",
    sourceSha256 = "af15", //incorrect but that is irrelevant
    runRequirements = Seq("openjdk"),
    buildRequirements = Seq(),
    testCommands = Seq("test --version"),
    homeUrl="test.example.com/index.html",
    license = "MIT",
    summary= "test is a tool that is tested in this test suite.",
    defaultJavaOptions = Seq("-Xms25m"),
    buildNumber = 0,
    notes = Some("This is java"))

  @Test
  def testYaml(): Unit = {
    val metaYaml = testRecipe.metaYaml
    println(metaYaml)

  }
}
