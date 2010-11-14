package de.downgra.scarg

import collection.mutable.{Buffer, ListBuffer, Stack => MStack}
import annotation.tailrec

class DoubleArgumentException(val message: String) extends RuntimeException(message)
class BadArgumentOrderException(val message: String) extends RuntimeException(message)

abstract class ArgumentParser[T](configFactory: ValueMap => T) extends ArgumentContainer
                                                                  with DefaultHelpViewer
                                                                  with ArgumentBuilders {

  /** the parse result, either left with a list of error messages, or right with the value created by `configFactory` */
  type ParseResult = Either[List[ParseError], T]

  override protected[scarg] val arguments = new ListBuffer[Argument]

  val optionDelimiters = ":="
  val errorOnUnknownArgument = true

  /** if true, show usage and error message after parsing arguments */
  val showErrors = true

  /** the default value for flags (first if flag is given, else second) */
  val flagDefaults = ("true", "false")
  
  @throws(classOf[DoubleArgumentException])
  @throws(classOf[BadArgumentOrderException])
  override protected[scarg] def addArgument(arg: Argument) = {
    arg match {
      case PositionalArgument(name,_,optional,_,_) =>
        // check double entries
        if(positionalArguments exists (_.name == name))
          throw new DoubleArgumentException("Positional argument %s already exists." format (name))
        // no required argument after an optional
        if(!optional && positionalArguments.exists(_.optional == true))
          throw new BadArgumentOrderException(
            "Required positional arguments are not allowed after optional positional arguments (%s)" format (name))
      case OptionArgument(names,valueName,default,_,_) =>
        // check double entries
        if(optionArguments exists (a => (a.names intersect names).nonEmpty))
          throw new DoubleArgumentException("Positional argument %s already exists." format (names))
        // no more option arguments allowed after positionals
        if(positionalArguments nonEmpty)
          throw new BadArgumentOrderException("After repeated positional arguments are no more arguments allowed (%s)" format (names))
      case _ =>
    }
    // no more arguments allowed after repeated positional
    if(positionalArguments exists (_.repeated))
      throw new BadArgumentOrderException("After a repeated positional argument are no more arguments allowed")
    arguments += arg
  }

  private object Delimiter {
    def unapply(s: String): Option[(String, String)] =
      s.span(!optionDelimiters.contains(_)) match {
        case (a, b) if a.length > 0 && b.length > 0 => Some((a, b.tail))
        case _                      => None
      }
  }

  def parse(args: Seq[String]): ParseResult = {
    val options = Map() ++ (optionArguments filter (_.valueName.isDefined) flatMap (o => o.names map ((_ -> o))))
    val flags = Map() ++ (optionArguments filter (_.valueName.isEmpty) flatMap (o => o.names map ((_ -> o))))
    val positionals = new MStack[PositionalArgument].pushAll(positionalArguments.reverse)

    var repeatedPositionalsFound: Set[PositionalArgument] = Set()
    var argumentsFound: Set[OptionArgument] = Set()
    var errors: List[ParseError] = List()
    var result: ValueMap = Map()

    @tailrec def _parse(args: Seq[String]): Unit = args.toList match {
      // ___ -f value
      case o :: v :: t if(options.contains(o) && v(0) != '-') =>
        options get(o) map { a =>
          result += (a.key -> (v :: result.getOrElse(a.key, Nil)))
          argumentsFound += a
        }
        _parse(t)
      // ___ -f[:=]value
      case Delimiter(o, v) :: t if(options contains o) =>
        options get(o) map { a =>
          result += (a.key -> (v :: result.getOrElse(a.key, Nil)))
          argumentsFound += a
        }
        _parse(t)
      // ___ -f
      case f :: t if(flags contains f) =>
        flags get(f) map { a => 
          // flags are booleans per default
          result += (a.key -> (flagDefaults._1 :: result.getOrElse(a.key, Nil)))
          argumentsFound += a
        }
        _parse(t)
      // ___ positionalParam
      case p :: t if(positionals.nonEmpty && p(0) != '-') =>
        val a = positionals.head
        // remember repeated positional arguments for later check
        if(!a.repeated) positionals.pop else repeatedPositionalsFound += a
        result += (a.key -> (p :: result.getOrElse(a.key, Nil)))
        _parse(t)
      // ___ unknown param
      case o :: t =>
        if(errorOnUnknownArgument) {
          errors = UnknownArgument(o) :: errors
        }
        _parse(t)
      case Nil =>
    }

    _parse(args)

    val notFoundOptions = optionArguments.toSet -- argumentsFound

    // need to set default for not given flags with value "false"
    notFoundOptions filter (_.valueName.isEmpty) foreach { a =>
      result += (a.key -> (flagDefaults._2 :: result.getOrElse(a.key, Nil)))
    }

    // set default values for all option arguments
    notFoundOptions filter (o => o.default.isDefined && o.valueName.nonEmpty) foreach { a => 
      result += (a.key -> (a.default.get :: result.getOrElse(a.key, Nil)))
    }

    // set default values for optional repeated positional values
    val missingPositionals = positionals.toSet -- repeatedPositionalsFound // remove found repeated arguments
    missingPositionals filter(p => p.optional && p.repeated) foreach { a =>
      result += (a.key -> Nil)
    }

    // check if all necessary arguments are given
    errors = (notFoundOptions filter (o => o.default.isEmpty && o.valueName.isDefined) // only options which are not flags
             ).foldLeft(errors)((a,v) => MissingPositional(v.names(0)) :: a)

    // check missing positional arguments without the optional or found repeated
    errors = (missingPositionals filter(p => p.optional == false)).foldLeft(errors)((a,v) => MissingPositional(v.name) :: a)

    if(errors.nonEmpty) {
      if(showErrors) {
        showUsage
        showErrors(errors.reverse)
      }
      Left(errors.reverse)
    } else Right(configFactory(result mapValues (_.reverse)))
  }
}
