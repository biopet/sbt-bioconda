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

import nl.biopet.bioconda.BiocondaPlugin.autoImport._
import sbt._
import sbt.Keys._

object BiocondaDefaults {
  def defaultSummary: Def.Initialize[Task[String]] =
    Def.task {
      s"""This summary for ${(name in Bioconda).value} is automatically generated.
         |Please visit ${(homepage in Bioconda).value.getOrElse(
           "the project's website")}
         |for more information about this program.
         |""".stripMargin.replace("\n", " ")
    }

  def defaultNotes: Def.Initialize[Task[String]] =
    Def.task {
      def javaOpts: String = {
        val javaDefaults = biocondaDefaultJavaOptions.value
        val builder = new StringBuilder
        javaDefaults.foreach(x => builder.append(x + " "))
        val opts = builder.toString().trim
        if (javaDefaults.isEmpty) "no default java option"
        else opts
      }
      s"""${(name in Bioconda).value} is a Java program that comes with a custom wrapper shell script.
         |By default '$javaOpts' is set in the wrapper.
         |The command that runs the program is '${biocondaCommand.value}.'
         |If you want to overwrite it you can specify memory options directly after your binaries.
         |If you have _JAVA_OPTIONS set globally this will take precedence.
         |For example run it with '${biocondaCommand.value} -Xms512m -Xmx1g'.
         |""".stripMargin.replace("\n", " ")
    }
  def defaultPullRequestTitle: Def.Initialize[Task[String]] =
    Def.task {
      if (biocondaIsReleased.value)
        s"New version for ${(name in Bioconda).value}"
      else s"New tool: ${(name in Bioconda).value}"
    }

  def defaultPullRequestBody: Def.Initialize[Task[String]] =
    Def.task {
      pullRequestTemplate(biocondaIsReleased.value) +
        s"""This pull request was automatically generated by [sbt-bioconda](https://github.com/biopet/sbt-bioconda).
         |Any releases of [${(name in Bioconda).value}](${(homepage in Bioconda).value
             .getOrElse("")})
         |that are not in bioconda are automatically generated if the `sbt biocondaRelease` command is issued.
         |As part of this process the recipes are tested using CircleCi as described
         |[here](https://bioconda.github.io/contribute-a-recipe.html#test-locally).
         |""".stripMargin
          .replace("\n", " ") + "\n\n***\n\n### Tool summary\n\n" + biocondaSummary.value
    }

  def pullRequestTemplate(released: Boolean): String = {
    def cross(yes: Boolean): String = if (yes) "x" else " "
    s"""|* [${cross(!released)}] This PR adds a new recipe.
        |* [x] AFAIK, this recipe **is directly relevant to the biological sciences** (otherwise, please submit to the more general purpose [conda-forge channel](https://conda-forge.org/docs/)).
        |* [${cross(released)}] This PR updates an existing recipe.
        |* [ ] This PR does something else (explain below).
        |***
        |""".stripMargin
  }
}
