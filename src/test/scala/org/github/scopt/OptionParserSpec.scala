package org.github.scopt

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class OptionParserSpec extends FunSuite with ShouldMatchers {

  test("empty argument list on empty option parser") {
    class OP extends OptionParser
    val op = new OP
    op.parse(List()) should be (true)
  }

  test("non-empty argument list on empty option parser should fail") {
    class OP extends OptionParser
    val op = new OP
    op.parseRaw(List("foo")) should not be ('empty)
    op.parseRaw(List("-foo")) should not be ('empty)
    op.parseRaw(List("--foo")) should not be ('empty)
    op.parseRaw(List("--foo", "bar")) should not be ('empty)
  }

  test("single required positional argument should parse") {
    class OP extends OptionParser {
      var V: Option[String] = None
      + "required" % "description" --> {s => V = Some(s)}
    }
    val op = new OP

    op.positionalArguments should have length (1)
    op.positionalArguments(0) should have(
      'name ("required"),
      'description ("description"),
      'optional (false)
    )

    op.parseRaw(Nil) should not be ('empty)
    op.V should be (None)
    op.parseRaw(List("--foo")) should not be ('empty)
    op.V should be (None)
    op.parseRaw(List("foo")) should be ('empty)
    op.V should be (Some("foo"))
  }

  test("single optional positional argument should parse") {
    class OP extends OptionParser {
      var V: Option[String] = None
      ~ "optional" % "description" --> {s => V = Some(s)}
    }
    val op = new OP

    op.positionalArguments should have length (1)
    op.positionalArguments(0) should have(
      'name ("optional"),
      'description ("description"),
      'optional (true)
    )

    op.parseRaw(Nil) should be ('empty)
    op.V should be (None)
    op.parseRaw(List("--foo")) should not be ('empty)
    op.V should be (None)
    op.parseRaw(List("foo")) should be ('empty)
    op.V should be (Some("foo"))
  }
}
