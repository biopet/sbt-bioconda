package nl.biopet.bioconda.schema

import play.api.libs.json.{JsValue, Json}
import org.yaml.snakeyaml.Yaml
import

case class BiocondaMetaYaml(package_info: Package,
                            source: Source,
                            build: Build,
                            requirements: Requirements,
                            about: About,
                            extra: Extra,
                            test: TestKey) {
  def toYaml: String = {
    val yaml = new Yaml()
    yaml.dump(this)
  }

}
