package nl.biopet.bioconda

import sbt._

trait BiocondaKeys {
  lazy val bioconda = config("bioconda") describedAs ("Configuration for bioconda repo")
  lazy val biocondaMainGitUrl = SettingKey[String]("Git URL of biocondaMain")
  lazy val biocondaMainBranch =
    SettingKey[String]("The default development branch on bioconda main.")
  lazy val biocondaGitUrl =
    SettingKey[String]("Git URL of your/your project's bioconda repo fork")
  lazy val biocondaBranch =
    SettingKey[String]("Branch for bioconda tool repository")
  lazy val biocondaUpdatedRepository =
    taskKey[File]("Clone the biocondaLocalRepo")
  lazy val biocondaRepository =
    settingKey[File](
      "Sandbox environment where the bioconda git branch is checked out.")
}
