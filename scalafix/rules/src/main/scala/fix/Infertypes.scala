package fix

import java.util

import metaconfig.Configured
import scalafix.internal.util.Pretty
import scalafix.internal.v1.DocumentFromProtobuf
import scalafix.patch.Patch
import scalafix.util.TokenOps
import scalafix.v1._

import scala.collection.mutable
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
    println(s"doc.synthetics.toList = ${doc.synthetics.toList}")

//    doc.synthetics.toList.foreach{case t: SemanticTree => println(t.structure)}
    doc.synthetics.toList.foreach{case t: SemanticTree => println(t.toString)}
    doc.tree.collect {
      case t@Defn.Val(mods, Pat.Var(name) :: Nil, None, body) => {
        fixDefinition(t, name, body)
      }
      case t@Defn.Var(mods, Pat.Var(name) :: Nil, None, Some(body)) =>
        fixDefinition(t, name, body)

      case t@Defn.Def(mods, name, _, _, None, body) =>
        fixDefinition(t, name, body)
      case t: Term => addSynthetic(t)
    }.flatten.asPatch
  }

  private def addSynthetic(term: Term)(implicit doc: SemanticDocument): Option[Patch] = {
    import scala.meta.Term._
    term match {
      case t@ForYield(value, term) =>
        None // we want to keep for comprehension as they are.
      case t@ApplyInfix(term, name, value, value1) => {
        None // we need to add . first, then the type
      }
      case t@Term.Apply(term, value) if t.synthetics.nonEmpty =>
        // for implicit
        Some(SyntheticHelper.buildPatch(t, t.synthetics))
      case t@Term.Apply(term, value) if t.synthetics.isEmpty && term.synthetics.nonEmpty =>
        // for .apply methods
        Some(SyntheticHelper.buildPatch(term, term.synthetics))

      case t@Term.ApplyType(term, value) if t.synthetics.nonEmpty =>
        // for implicit
        Some(SyntheticHelper.buildPatch(t, t.synthetics))
      case _ => None
    }
  }

  private def fixDefinition(defn: Defn, name: Term.Name, body: Term)(implicit doc: SemanticDocument): Option[Patch] = {
    for {
      (replace, spaces) <- getReplaceAndSpaces(defn, body)
      explicitType <- getTypeAsSeenFromGlobal(name)
      filteredType <- filterType(explicitType)
      //      _ = println(s"filteredType.prefixString = ${filteredType.prefix}")
    } yield Patch.addRight(replace, s"$spaces: ${filteredType.finalResultType}")
  }

  private def getTypeAsSeenFromGlobal(name: Term.Name)(implicit doc: SemanticDocument): Option[global.Type] = {
    for {
      context <- getContext(name)
      finalType = context.tree.symbol.info
    } yield finalType
  }

  private def filterType(finalType: global.Type): Option[global.Type] = {
    finalType match {
      case f if f.isInstanceOf[scala.reflect.runtime.universe.ConstantType] =>
        None // don't annotate ConstantTypes
      case f if f.toString().contains("#") && f.toString().contains(".type") =>
        None // don't annotate types that look like fix.WidenSingleType#strings.type
      //Todo: add a special case for structural type: remove implicit and replace lazy val by a def
      //Todo: deal with the root prefix to avoid cyclical types
      //Todo: remove super types: we don't infer them
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