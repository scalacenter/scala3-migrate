/*
rule = migrate.ParensAroundParam
*/
package parensaroundparam

// taken from https://github.com/ohze/scala-rewrites/blob/dotty/input/src/test/scala/fix/scala213/ParensAroundLambda.scala
class ParensAroundParam {
  Nil.foreach { x: Nothing => }
  Nil.foreach { (x: Nothing) => }
  val f = { s: String => s }
  val g = (s: String) => ()
  Map("a" -> 1).map { x: (String, Int) =>
    ???
  }

  Seq(1).map { i: Int =>
    i + 1
  }
  Seq(1).map { i => i + 1 }

  Seq(1).foreach { implicit i: Int => }
}
