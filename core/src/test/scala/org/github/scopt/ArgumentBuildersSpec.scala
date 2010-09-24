package org.github.scopt

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import collection.mutable.ListBuffer

class ArgumentBuildersSpec extends FunSuite with ShouldMatchers {

  trait TestContainer extends ArgumentContainer {
    override val arguments = new ListBuffer[Argument]
    override def addArgument(arg: Argument) = arguments += arg
  }

  test("complete positional") {
    object Test extends TestContainer with ArgumentBuilders {
      + "required1" |% "description1" |> {action => action}
      ~ "required2" |% "description2" |> {action => action}
    }

    Test.arguments should have length (2)
    Test.arguments(0) should have ('name ("required1"), 'description ("description1"), 'optional (false))
    Test.arguments(1) should have ('name ("required2"), 'description ("description2"), 'optional (true))
  }

  test("positional without description") {
    object Test extends TestContainer with ArgumentBuilders {
      + "required1" |> {action => action}
      ~ "required2" |> {action => action}
    }

    Test.arguments should have length (2)
    Test.arguments(0) should have ('name ("required1"), 'description (""), 'optional (false))
    Test.arguments(1) should have ('name ("required2"), 'description (""), 'optional (true))
  }

  test("complete option") {
    object Test extends TestContainer with ArgumentBuilders {
      ! "-f" | "--foo" |^ "valueName1" |* "defaultValue1" |% "description1" |> {action => action}
      ! "--oof" | "-o" |% "description2" |* "defaultValue2" |^ "valueName2" |> {action => action}
      ! "-b" |^ "valueName3" |* "defaultValue3" |% "description3" |> {action => action}
    }

    Test.arguments should have length (3)
    Test.arguments(0) should have ('names (List("-f", "--foo")), 'valueName (Some("valueName1")),
                                   'default (Some("defaultValue1")), 'description ("description1"))
    Test.arguments(1) should have ('names (List("--oof", "-o")), 'valueName (Some("valueName2")),
                                   'default (Some("defaultValue2")), 'description ("description2"))
    Test.arguments(2) should have ('names (List("-b")), 'valueName (Some("valueName3")),
                                   'default (Some("defaultValue3")), 'description ("description3"))
  }

  test("option without description") {
    object Test extends TestContainer with ArgumentBuilders {
      ! "-f" | "--foo" |^ "valueName1" |* "defaultValue1" |> {action => action}
      ! "--bar" |^ "valueName2" |* "defaultValue2" |> {action => action}
    }

    Test.arguments should have length (2)
    Test.arguments(0) should have ('names (List("-f", "--foo")), 'valueName (Some("valueName1")),
                                   'default (Some("defaultValue1")), 'description (""))
    Test.arguments(1) should have ('names (List("--bar")), 'valueName (Some("valueName2")),
                                   'default (Some("defaultValue2")), 'description (""))
  }

  test("option without default value") {
    object Test extends TestContainer with ArgumentBuilders {
      ! "-f" | "--foo" |^ "valueName1" |% "description1" |> {action => action}
      ! "-b" |% "description2" |^ "valueName2" |> {action => action}
    }

    Test.arguments should have length (2)
    Test.arguments(0) should have ('names (List("-f", "--foo")), 'valueName (Some("valueName1")),
                                   'default (None), 'description ("description1"))
    Test.arguments(1) should have ('names (List("-b")), 'valueName (Some("valueName2")),
                                   'default (None), 'description ("description2"))
  }

  test("option without value name") {
    object Test extends TestContainer with ArgumentBuilders {
      ! "-f" | "--foo" |* "defaultValue1" |% "description1" |> {action => action}
      ! "--bar" |% "description2" |* "defaultValue2" |> {action => action}
    }

    Test.arguments should have length (2)
    Test.arguments(0) should have ('names (List("-f", "--foo")), 'valueName (None),
                                   'default (Some("defaultValue1")), 'description ("description1"))
    Test.arguments(1) should have ('names (List("--bar")), 'valueName (None),
                                   'default (Some("defaultValue2")), 'description ("description2"))
  }

  test("minimal option") {
    object Test extends TestContainer with ArgumentBuilders {
      ! "-f" |> {action => action}
    }

    Test.arguments should have length (1)
    Test.arguments(0) should have ('names (List("-f")), 'valueName (None),
                                   'default (None), 'description (""))
  }

  test("lot option names") {
    object Test extends TestContainer with ArgumentBuilders {
      ! "-f" | "-foo" | "--bar" | "--blubblub" |> {action => action}
    }

    Test.arguments should have length (1)
    Test.arguments(0) should have ('names (List("-f", "-foo", "--bar", "--blubblub")), 'valueName (None),
                                   'default (None), 'description (""))
  }

  test("option non-string default values") {
    object Test extends TestContainer with ArgumentBuilders {
      ! "-f" |* 42 |> {action => action}
      ! "-d" |* 23.42 |> {action => action}
      ! "-b" |* true |> {action => action}
    }

    Test.arguments should have length (3)
    Test.arguments(0) should have ('names (List("-f")), 'valueName (None),
                                   'default (Some("42")), 'description (""))
    Test.arguments(1) should have ('names (List("-d")), 'valueName (None),
                                   'default (Some("23.42")), 'description (""))
    Test.arguments(2) should have ('names (List("-b")), 'valueName (None),
                                   'default (Some("true")), 'description (""))
  }
}