package fix

import metaconfig.Configured
import scalafix.patch.Patch
import scalafix.util.TokenOps
import scalafix.v1._

import scala.meta._
import scala.meta.contrib.Trivia
import scala.meta.internal.pc.ScalafixGlobal
import scala.meta.internal.proxy.GlobalProxy
import scala.meta.tokens.Token
import scala.tools.nsc.reporters.StoreReporter
import scala.util.{Failure, Success, Try}

class Infertypes(global: ScalafixGlobal) extends SemanticRule("Infertypes") {
  override def description: String = "infer types"

  def this() = this(null)

  override def withConfiguration(config: Configuration): Configured[Rule] = {
    if (config.scalacClasspath.isEmpty) {
      Configured.error(s"config.scalacClasspath should not be empty")
    } else {
      val global = CompilerUtils.newGlobal(config.scalacClasspath, config.scalacOptions)
      global match {
        case Success(settings) => Configured.ok(new Infertypes(new ScalafixGlobal(settings, new StoreReporter, Map())))
        case Failure(exception) => Configured.error(exception.getMessage)
      }
    }
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case t @ Defn.Val(mods, Pat.Var(name) :: Nil, None, body) =>
        fixDefinition(t, name, body)

      case t @ Defn.Var(mods, Pat.Var(name) :: Nil, None, Some(body)) =>
        fixDefinition(t, name, body)

      case t @ Defn.Def(mods, name, _, _, None, body) =>
        fixDefinition(t, name, body)
    }.flatten.asPatch
  }

  private def fixDefinition(defn: Defn, name: Term.Name, body: Term)(implicit doc: SemanticDocument): Option[Patch] = {
    for {
      (replace, spaces) <- getReplaceAndSpaces(defn, body)
      explicitType <- getTypeAsSeenFromGlobal(name)
      filteredType <- filterType(explicitType)
      _ = println(s"filteredType.prefixString = ${filteredType.prefix}")

    } yield Patch.addRight(replace, s"$spaces: ${filteredType.finalResultType}")
  }

  private def getTypeAsSeenFromGlobal(name: Term.Name)(implicit doc: SemanticDocument): Option[global.Type] = {
    for {
      context <-  getContext(name)
      finalType =  context.tree.symbol.info
    } yield finalType
  }

  private def filterType(finalType: global.Type): Option[global.Type] = {
    finalType match {
      case f if f.isInstanceOf[scala.reflect.runtime.universe.ConstantType] =>
        None // don't annotate ConstantTypes
      case f if f.toString().contains("#") && f.toString().contains(".type") =>
        None // don't annotate types that look like fix.WidenSingleType#strings.type
      case f => Some(f)
    }
  }


  private def getReplaceAndSpaces(defn: Defn, body: Term)(implicit doc: SemanticDocument): Option[(Token, String)] = {
    val tokens = doc.tokenList
    import tokens._

    for {
      start <- defn.tokens.headOption
      end <- body.tokens.headOption
      lhsTokens = slice(start, end)
      replace <- lhsTokens.reverseIterator.find(x =>
        !x.is[Token.Equals] && !x.is[Trivia]
      )
      space = {
        if (TokenOps.needsLeadingSpaceBeforeColon(replace)) " "
        else ""
      }
    } yield (replace, space)
  }

  private def getContext(name: Term.Name)(implicit doc: SemanticDocument): Option[global.Context] = {
    val unit = global.newCompilationUnit(doc.input.text, doc.input.syntax)
    val gpos = unit.position(name.pos.start)
    GlobalProxy.typedTreeAt(global, gpos)
    Try(global.doLocateContext(gpos)).toOption
  }
}