package nl.biopet.bioconda

package object schema {
  case class Package(name: String,
                     version: String) {}
  case class Source(url: String,
                    sha256: String) {}
  case class Build(number: Int) {}
  case class Requirements(run: Seq[String],
                         build: Seq[String]) {}
  case class About(home: String,
                   license: String,
                   summary: String) {}
  case class Extra(notes: Option[String]) {}
  case class TestKey(commands: Seq[String])

}