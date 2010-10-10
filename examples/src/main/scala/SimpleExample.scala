package examples

import de.downgra.scarg.{ArgumentParser, ConfigMap, ValueMap}

/**
 * usage: SimpleExample [options] infile
 *
 * options:
 *   -v, --verbose
 *   -o OUT
 *   infile
 */
object SimpleExample {

  class Configuration(m: ValueMap) extends ConfigMap(m) {
    val verbose = ("verbose", false).as[Boolean]
    val outfile = ("outfile", "-").as[String]
    val infile = ("infile", "").as[String]
  }

  case class SimpleParser() extends ArgumentParser(new Configuration(_)) {
    override val programName = Some("SimpleExample")

    ! "-v" | "--verbose"   |% "active verbose output"            |> "verbose"
    ! "-o" |^ "OUT" |* "-" |% "output filename, default: stdout" |> 'outfile
    ("-" >>> 50)
    + "infile"             |% "input filename"                   |> 'infile
  }

  def main(args: Array[String]) {
    SimpleParser().parse(args) match {
      case Left(xs) =>
        println("error: " + xs)
      case Right(c) =>
        println("verbose: " + c.verbose)
        println("outfile: " + c.outfile)
        println(" infile: " + c.infile)
    }
  }

}

// vim: set ts=2 sw=2 et:
