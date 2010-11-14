package examples

import de.downgra.scarg.{ArgumentParser, ConfigMap, ValueMap, DefaultHelpViewer}

/**
 * usage: SimpleExample [options] infile
 *
 * options:
 *   -v, --verbose
 *   -o OUT
 *   infile
 */
object AlternateSyntaxExample {

  class Configuration(m: ValueMap) extends ConfigMap(m) {
    val verbose = get[Boolean]("verbose") getOrElse false
    val outfile = get[String]("outfile") getOrElse "-"
    val infile = ("infile", "").as[String]
  }

  case class SimpleParser() extends ArgumentParser(new Configuration(_))
                               with DefaultHelpViewer {
    override val programName = Some("AlternateSyntaxExample")

    newOptional("-v").name("--verbose").description("active verbose output").
                      key("verbose")
    newOptional("-o").valueName("OUT").description("output filename, default: stdout").
                      key("outfile")

    newSeparator("-", 50)

    newPositional("infile").required.description("input filename").key("infile")
  }

  def main(args: Array[String]) {
    SimpleParser().parse(args) match {
      case Right(c) =>
        println("verbose: " + c.verbose)
        println("outfile: " + c.outfile)
        println(" infile: " + c.infile)
      case Left(xs) =>
    }
  }

}

// vim: set ts=2 sw=2 et:
