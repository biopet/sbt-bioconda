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

import nl.biopet.utils.conversions.anyToJson
import nl.biopet.utils.io.stringToFile
import org.yaml.snakeyaml.Yaml

import scala.collection.immutable.ListMap
import scala.io.Source

class BiocondaRecipe(name: String,
                     version: String,
                     command: String,
                     sourceUrl: String,
                     sourceSha256: String,
                     runRequirements: Seq[String],
                     buildRequirements: Seq[String],
                     testCommands: Seq[String],
                     homeUrl: String,
                     license: String,
                     summary: String,
                     defaultJavaOptions: Seq[String],
                     buildNumber: Int = 0,
                     description: Option[String] = None,
                     notes: Option[String] = None,
                     doi: Option[String] = None) {

  /**
    * The filename for the jar as determined by the sourceUrl
    * @return a filename as a string
    */
  def fileName: String = sourceUrl.split("/").lastOption.getOrElse(s"$name.jar")

  /**
    * The filename for the wrapper
    * @return the filename
    */
  def wrapperFilename: String = s"$name.py"

  /**
    * Creates meta.yml, build.sh and the wrapper in dir
    * @param dir the directory where your recipe is created.
    */
  def createRecipeFiles(dir: File): Unit = {
    dir.mkdirs()
    val buildSh = new File(dir, "build.sh")
    val meta = new File(dir, "meta.yaml")
    val wrapper = new File(dir, wrapperFilename)
    stringToFile(metaYaml, meta)
    stringToFile(buildScript, buildSh)
    stringToFile(wrapperScript, wrapper)
    // Wrapper should be executable
    wrapper.setExecutable(true)
  }

  /**
    * Creates the meta.yaml as a string.
    * @return meta.yaml as a string.
    */
  def metaYaml: String = {

    val doiMap: ListMap[String, String] = doi match {
      case Some(d) => ListMap("doi" -> d)
      case _       => ListMap()
    }

    val notesMap: ListMap[String, String] = notes match {
      case Some(n) => ListMap("notes" -> n)
      case _       => ListMap()
    }
    val extraMap: ListMap[String, ListMap[String, String]] = {
      val extra: ListMap[String, String] = notesMap ++ doiMap
      if (extra.isEmpty) ListMap()
      else ListMap("extra" -> extra)
    }

    val descriptionMap: ListMap[String, String] = description match {
      case Some(d) => ListMap("description" -> d)
      case _       => ListMap()
    }

    val meta: ListMap[String, Any] = {
      ListMap(
        "package" -> ListMap(
          "name" -> name,
          "version" -> version
        ),
        "source" -> ListMap(
          "url" -> sourceUrl,
          "sha256" -> sourceSha256
        ),
        "build" -> ListMap(
          "number" -> buildNumber
        ),
        "requirements" -> ListMap(
          "run" -> (runRequirements ++ Seq("python")),
          "build" -> buildRequirements
        ),
        "about" -> {
          ListMap(
            "home" -> homeUrl,
            "license" -> license,
            "summary" -> summary
          ) ++ descriptionMap
        },
        "test" -> ListMap(
          "commands" -> testCommands
        )
      ) ++ extraMap
    }
    val yaml = new Yaml()

    // The java map implementation is needed to preserve order.
    val javaMap: java.util.LinkedHashMap[String, String] = {
      val newmap: java.util.LinkedHashMap[String, String] =
        new java.util.LinkedHashMap()
      meta.foreach(x => newmap.put(x._1, yaml.load(anyToJson(x._2).toString())))
      newmap
    }

    s"""# Based on OpenJDK recipe in conda-forge
       |# https://github.com/conda-forge/openjdk-feedstock/blob/master/recipe/meta.yaml
       |#
       |# !! THIS FILE WAS AUTOMATICALLY GENERATED BY THE SBT-BIOCONDA PLUGIN !!
       |# !!                       DO NOT EDIT MANUALLY                       !!
       |
       |""".stripMargin + yaml.dumpAsMap(javaMap)
  }

  /**
    * Creates the buildscript as string.
    * @return the buildscript.
    */
  def buildScript: String =
    s"""#!/usr/bin/env bash
       |# Build file is copied from VarScan
       |# https://github.com/bioconda/bioconda-recipes/blob/master/recipes/varscan/build.sh
       |# This file was automatically generated by the sbt-bioconda plugin.
       |
       |outdir=$$PREFIX/share/$$PKG_NAME-$$PKG_VERSION-$$PKG_BUILDNUM
       |mkdir -p $$outdir
       |mkdir -p $$PREFIX/bin
       |cp $fileName $$outdir/$fileName
       |cp $$RECIPE_DIR/$wrapperFilename $$outdir/$command
       |ln -s $$outdir/$command $$PREFIX/bin
       |""".stripMargin

  /**
    * Creates the wrapperscript as a string.
    * @return the wrappercript.
    */
  def wrapperScript: String = {
    def pyScript: String = {
      val source = getClass.getClassLoader
        .getResourceAsStream("nl/biopet/bioconda/wrapper.py")
      Source.fromInputStream(source).mkString
    }

    val javaOpts = new StringBuilder
    javaOpts.append("[")
    defaultJavaOptions.foreach(x => javaOpts.append("'" + x + "',"))
    javaOpts.append("]")
    s""" |#!/usr/bin/env python
         |#
         |# Wrapper script for starting the $name JAR package
         |#
         |# This script is written for use with the Conda package manager and is copied
         |# from the peptide-shaker wrapper. Only the parameters are changed.
         |# (https://github.com/bioconda/bioconda-recipes/blob/master/recipes/peptide-shaker/peptide-shaker.py)
         |#
         |# This file was automatically generated by the sbt-bioconda plugin.
         |
         |import os
         |import subprocess
         |import sys
         |import shutil
         |
         |from os import access
         |from os import getenv
         |from os import X_OK
         |
         |jar_file = '$fileName'
         |
         |default_jvm_mem_opts = $javaOpts
         |
         |# !!! End of parameter section. No user-serviceable code below this line !!!
         |
       """.stripMargin + "\n" + pyScript
  }

}
