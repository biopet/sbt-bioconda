package nl.biopet.bioconda.schema

import play.api.libs.json.{Reads, Json, Writes}
object Implicits {
  implicit val packageReads: Reads[Package] =
    Json.reads[Package]
  implicit val sourceReads: Reads[Source] = Json.reads[Source]
  implicit val buildReads: Reads[Build] = Json.reads[Build]
  implicit val requirementsReads: Reads[Requirements] = Json.reads[Requirements]
  implicit val aboutReads: Reads[About] = Json.reads[About]
  implicit val extraReads: Reads[Extra] = Json.reads[Extra]
  implicit val testReads: Reads[TestKey] = Json.reads[TestKey]

  implicit val packageWrites: Writes[Package] = Json.writes[Package]
  implicit val sourceWrites: Writes[Source] = Json.writes[Source]
  implicit val buildWrites: Writes[Build] = Json.writes[Build]
  implicit val requirementsWrites: Writes[Requirements] = Json.writes[Requirements]
  implicit val aboutWrites: Writes[About] = Json.writes[About]
  implicit val extraWrites: Writes[Extra] = Json.writes[Extra]
  implicit val testWrites: Writes[TestKey] = Json.writes[TestKey]

}
