package nl.biopet.tools.dummytool

import java.io.File

import nl.biopet.utils.tool.{AbstractOptParser, ToolCommand}

class ArgsParser(toolCommand: ToolCommand[Args])
    extends AbstractOptParser[Args](toolCommand) {
  opt[File]("inputFile")
    .abbr("i")
    .unbounded()
    .required()
    .maxOccurs(1)
    .action((x, c) => c.copy(inputFile = x))
    .text("NonEmptyDescription")
}
