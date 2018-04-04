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

import java.io.File

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

import scala.io.Source
class BiocondaRecipeTest extends TestNGSuite with Matchers {

  val testRecipe = new BiocondaRecipe(
    name = "test",
    version = "1.0",
    command = "testing",
    sourceUrl = "test.example.com/test.jar",
    sourceSha256 = "af15", //incorrect but that is irrelevant
    runRequirements = Seq("openjdk"),
    buildRequirements = Seq(),
    testCommands = Seq("testing --version"),
    homeUrl = "test.example.com/index.html",
    license = "MIT",
    summary = "test is a tool that is tested in this test suite.",
    defaultJavaOptions = Seq("-Xms25m"),
    buildNumber = 0,
    notes = Some("This is java")
  )

  @Test
  def testCreateRecipeFiles(): Unit = {
    val tmp = File.createTempFile("recipe", "dir")
    // The file should be a dir.
    tmp.delete()
    tmp.mkdir()
    testRecipe.createRecipeFiles(tmp)
    val metaYaml = new File(tmp, "meta.yaml")
    val buildSh = new File(tmp, "build.sh")
    val wrapper = new File(tmp, testRecipe.wrapperFilename)
    metaYaml should exist
    buildSh should exist
    wrapper should exist
    assert(wrapper.canExecute)
    assert(!buildSh.canExecute)
    assert(!metaYaml.canExecute)
    Source.fromFile(metaYaml).mkString should equal(testRecipe.metaYaml + "\n")
    Source.fromFile(buildSh).mkString should equal(
      testRecipe.buildScript + "\n")
    Source.fromFile(wrapper).mkString should equal(
      testRecipe.wrapperScript + "\n")
  }

  @Test
  def testRecipeValues(): Unit = {
    testRecipe.fileName shouldBe ("test.jar")
    testRecipe.wrapperFilename shouldBe ("test.py")
  }

  @Test
  def testMetaYaml(): Unit = {
    val yaml = testRecipe.metaYaml
    yaml should include("package:")
    yaml should include("  name: test")
    yaml should include("  version: '1.0'")
    yaml should include("source:")
    yaml should include("  sha256: af15")
    yaml should include("  url: test.example.com/test.jar")
    yaml should include("about:")
    yaml should include("  home: test.example.com/index.html")
    yaml should include("  license: MIT")
    yaml should include(
      "  summary: test is a tool that is tested in this test suite.")
    yaml should include("requirements:")
    yaml should include("  run:")
    yaml should include("- openjdk")
    yaml should include("  build:")
    yaml should include("test:")
    yaml should include("  commands:")
    yaml should include("- testing --version")
    yaml should include("extra:")
    yaml should include("notes: This is java")
    yaml shouldNot include("Nederlandse tekst ")
  }

  @Test
  def testPythonWrapper(): Unit = {
    val py = testRecipe.wrapperScript

    py should include("jar_file = 'test.jar'")
    py should include("default_jvm_mem_opts = ['-Xms25m',]")
    py should include("#!/usr/bin/env python")
    py should include("if __name__ == '__main__':")
    py should not include ("Perl the best thing since sliced bread")
  }

  @Test
  def testBuildScript(): Unit = {
    val build = testRecipe.buildScript
    build should include("#!/usr/bin/env bash")
    build should include(
      "outdir=$PREFIX/share/$PKG_NAME-$PKG_VERSION-$PKG_BUILDNUM")
    build should include("mkdir -p $outdir")
    build should include("mkdir -p $PREFIX/bin")
    build should include("cp test.jar $outdir/test.jar")
    build should include("cp $RECIPE_DIR/test.py $outdir/testing")
    build should include("ln -s $outdir/testing $PREFIX/bin")
    build should not include ("Some crazy text message")
  }
}
