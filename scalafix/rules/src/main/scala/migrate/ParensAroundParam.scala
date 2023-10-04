package migrate

import scala.meta._
import scala.meta.tokens.Token._

import scalafix.v1._

// taken from https://github.com/ohze/scala-rewrites/blob/dotty/rewrites/src/main/scala/fix/scala213/ParensAroundLambda.scala
class ParensAroundParam extends SyntacticRule("migrate.ParensAroundParam") {
  override def fix(implicit doc: SyntacticDocument): Patch =
    doc.tree.collect { case fun @ Term.Function(List(param @ Term.Param(_, _, Some(_), _)), _) =>
      fun.tokens.head match {
        case _: LeftParen  => Patch.empty
        case _: KwImplicit => Patch.empty
        case _             => Patch.addAround(param, "(", ")")
      }
    }.asPatch
}
