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

import org.scalatest.Matchers
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import nl.biopet.bioconda.BiocondaDefaults.pullRequestTemplate

class BiocondaDefaultsTest extends TestNGSuite with Matchers {

  @Test
  def testPullRequestTemplate: Unit = {
    pullRequestTemplate(true) shouldBe
      s"""|* [ ] This PR adds a new recipe.
          |* [x] AFAIK, this recipe **is directly relevant to the biological sciences** (otherwise, please submit to the more general purpose [conda-forge channel](https://conda-forge.org/docs/)).
          |* [x] This PR updates an existing recipe.
          |* [ ] This PR does something else (explain below).
          |***
          |""".stripMargin

    pullRequestTemplate(false) shouldBe
      s"""|* [x] This PR adds a new recipe.
          |* [x] AFAIK, this recipe **is directly relevant to the biological sciences** (otherwise, please submit to the more general purpose [conda-forge channel](https://conda-forge.org/docs/)).
          |* [ ] This PR updates an existing recipe.
          |* [ ] This PR does something else (explain below).
          |***
          |""".stripMargin
  }
}
