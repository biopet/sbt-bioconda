package nl.biopet.tools.dummytool

import nl.biopet.utils.tool.ToolCommand

object DummyTool extends ToolCommand[Args] {
  def emptyArgs = Args()
  def argsParser = new ArgsParser(this)

  def main(args: Array[String]): Unit = {
    val cmdArgs = cmdArrayToArgs(args)

    logger.info("Start")
    logger.info("Done")
  }
  val loremIpsum: String =
    """Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                              |Aliquam bibendum tellus sed lectus tristique egestas.
                              |Aenean malesuada lacus sed mollis hendrerit. Aliquam ac mollis sapien.
                              |Donec vel suscipit dui. Aenean pretium nibh in pulvinar consequat.
                              |Duis feugiat mattis erat, sed varius lectus eleifend vel.
                              |Etiam feugiat neque a dolor ornare pulvinar.
                              |
                              |Aenean id nibh mi.Fusce vel dapibus dui, quis dapibus felis.
                              |Aenean ipsum purus, bibendum a odio non, mattis efficitur dui.
                              |In fermentum est faucibus, bibendum urna sollicitudin, tempor erat.
                              |Vivamus aliquet nulla enim, non pharetra dui pulvinar id.
                              |Aliquam erat volutpat. Morbi tincidunt iaculis viverra.
                              |Suspendisse eget metus at lorem varius feugiat. Aliquam erat volutpat.
                              |Aliquam consequat nibh ut feugiat condimentum.
                              |Pellentesque aliquam cursus ex, ac consequat est viverra vitae.
                              |Donec purus orci, efficitur vel sem a, sodales aliquam tellus.
                              |Maecenas at leo posuere, tempus risus in, sodales ligula.
                              |Nam mattis enim a ligula iaculis vulputate. Nam fringilla.
                              """.stripMargin

  def descriptionText: String = loremIpsum.substring(0, 250)

  def manualText: String =
    s"""
      |${loremIpsum.substring(0, 250)} Example:
      |${example("-i", "<input_file>")}
    """.stripMargin

  def exampleText: String = loremIpsum.substring(0, 250)
}
