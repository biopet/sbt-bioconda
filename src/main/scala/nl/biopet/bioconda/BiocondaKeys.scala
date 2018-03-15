package nl.biopet.bioconda

import sbt._

trait BiocondaKeys {
  lazy val bioconda = config("bioconda") describedAs ("Configuration for bioconda repo")
  lazy val biocondaMainGitUrl = settingKey[String]("Git URL of biocondaMain")
  lazy val biocondaMainBranch =
    settingKey[String]("The default development branch on bioconda main.")
  lazy val biocondaGitUrl =
    settingKey[String]("Git URL of your/your project's bioconda repo fork")
  lazy val biocondaBranch =
    settingKey[String]("Branch for bioconda tool repository")
  lazy val biocondaUpdatedRepository =
    taskKey[File]("Clone the biocondaLocalRepo")
  lazy val biocondaRepository =
    settingKey[File](
      "Sandbox environment where the bioconda git branch is checked out.")
}
