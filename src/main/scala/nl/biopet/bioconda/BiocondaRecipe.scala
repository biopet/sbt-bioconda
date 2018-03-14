package nl.biopet.bioconda

import org.yaml.snakeyaml.Yaml

class BiocondaRecipe(name: String,
                     version: String,
                     sourceUrl: String,
                     sourceSha256: String,
                     runRequirements: List[String],
                     buildRequirements: List[String],
                     homeUrl: String,
                     license: String,
                     summary: String,
                     description: String = "",
                     buildNumber: Int = 0,
                     notes: String = "") {

  def fileName: String = sourceUrl.split("/").last

  def metaYaml: String =
    s"""# Based on OpenJDK recipe in conda-forge
       |# https://github.com/conda-forge/openjdk-feedstock/blob/master/recipe/meta.yaml
       |
       |{% set name = "$name" %}
       |{% set version = "$version" % }
       |
       |package:
       |  name: "{{ name }}"
       |  version: "{{ version }}"
       |
       |source:
       |  url: "$sourceUrl"
       |  sha256: "$sourceSha256"
       |
       |build:
       |  number: "$buildNumber"
       |
       |about:
       |  home: "$homeUrl"
       |  license: "$license"
       |  summary: "$summary"
       |  description: |
       |    $description
     """.stripMargin

  def buildScript: String =
    s"""#!/usr/bin/env bash
       |# Build file is copied from VarScan
       |# https://github.com/bioconda/bioconda-recipes/blob/master/recipes/varscan/build.sh
       |
       |outdir=$$PREFIX/share/$$PKG_NAME-$$PKG_VERSION-$$PKG_BUILDNUM
       |mkdir -p $$outdir
       |mkdir -p $$PREFIX/bin
       |cp $fileName $$outdir/$fileName
       |cp $$RECIPE_DIR/${Wrapper.file} $$outdir/$name
     """.stripMargin

  object Wrapper {
    def file = s"$name.py"
    def script: String = {
      val wrapperStream =
        getClass.getResourceAsStream("nl/biopet/bioconda/wrapper.py")

      s"""
         |#!/usr/bin/env python
         |#
         |# Wrapper script for starting the Biopet JAR package
         |#
         |# This script is written for use with the Conda package manager and is copied
         |# from the peptide-shaker wrapper. Only the parameters are changed.
         |# (https://github.com/bioconda/bioconda-recipes/blob/master/recipes/peptide-shaker/peptide-shaker.py)
         |#
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
         |default_jvm_mem_opts = ['-Xms512m', '-Xmx2g']
         |
         |# !!! End of parameter section. No user-serviceable code below this line !!!
         |
       """.stripMargin + scala.io.Source.fromInputStream(wrapperStream).mkString
    }

  }
}
