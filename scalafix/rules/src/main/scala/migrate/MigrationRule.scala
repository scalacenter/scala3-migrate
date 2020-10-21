package migrate

import metaconfig.Configured
import scalafix.patch.Patch
import scalafix.util.TokenOps
import scalafix.v1._
import utils.ScalaExtensions._

import scala.meta._
import scala.meta.contrib.Trivia
import scala.meta.internal.pc.ScalafixGlobal
import scala.meta.tokens.Token
import scala.tools.nsc.reporters.StoreReporter
import scala.util.{Failure, Success}

class MigrationRule(g: ScalafixGlobal) extends SemanticRule("MigrationRule") {
  override def description: String = "infer types and show synthetics"

  def this() = this(null)

  override def withConfiguration(config: Configuration): Configured[Rule] = {
    if (config.scalacClasspath.isEmpty) {
      Configured.error(s"config.scalacClasspath should not be empty")
    } else {
      val global = CompilerService.newGlobal(config.scalacClasspath, config.scalacOptions)
      global match {
        case Success(settings) => Configured.ok(new MigrationRule(new ScalafixGlobal(settings, new StoreReporter, Map())))
        case Failure(exception) => Configured.error(exception.getMessage)
      }
    }
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    lazy implicit val unit = g.newCompilationUnit(doc.input.text, doc.input.syntax)

    val patchForExplicitResultTypes = addExplicitResultType()
    val patchForTypeApply = AddTypeApply()

    patchForExplicitResultTypes + patchForTypeApply
  }

  private def AddTypeApply()(implicit doc: SemanticDocument,
                             unit: g.CompilationUnit): Patch = {

    doc.synthetics.collect {
      case syn@TypeApplyTree(function: SemanticTree, typeArguments: List[SemanticType]) =>
        (for {
          originalTree <- SyntheticHelper.getOriginalTree(syn)
          if (!originalTree.isInstanceOf[Term.ApplyInfix])
          if (!(originalTree.isInstanceOf[Pat] && !originalTree.isInstanceOf[Term])) // Never add types on elements that are on the right side of "="
          replace <- if (syn.toString.startsWith("*.apply")) Some(".apply")  else if (syn.toString.startsWith("*[")) Some("") else None
          typesSeenFromGlobal = getTypeNameAsSeenByGlobal(originalTree, replace).map(_.map(_.dealiasWiden.toString()))
          types <- typesSeenFromGlobal
            .orElse(typeArguments.map(getAbsoluteType).sequence)
          // if we don't know how to express a type, we don't create a patch
          if types.length == typeArguments.length
        } yield Patch.addRight(originalTree, s"${replace}[${types.mkString(", ")}]")
          ).getOrElse(Patch.empty)
    }.toList.reverse.asPatch
  }


  private def getTypeNameAsSeenByGlobal(origin: Tree, replace: String)
                                       (implicit doc: SemanticDocument,
                                        unit: g.CompilationUnit): Option[List[g.Type]] = {
    for {
      globalOrigin <- CompilerService.getTreeInGlobal(origin.tokens.last, g, replace)
      types <- globalOrigin match {
        case g.TypeApply(fun, args) => Some(args.map(_.tpe.asInstanceOf[g.Type]))
        case _ => None
      }
    } yield types
  }

  private def addExplicitResultType()(implicit doc: SemanticDocument,
                                      unit: g.CompilationUnit): Patch = {
    doc.tree.collect {
      case t@Defn.Val(mods, Pat.Var(name) :: Nil, None, body) => {
        fixDefinition(t, name, body)
      }
      case t@Defn.Var(mods, Pat.Var(name) :: Nil, None, Some(body)) =>
        fixDefinition(t, name, body)

      case t@Defn.Def(mods, name, _, _, None, body) =>
        fixDefinition(t, name, body)
    }.asPatch
  }

  private def fixDefinition(defn: Defn, name: Term.Name, body: Term)
                           (implicit doc: SemanticDocument, unit: g.CompilationUnit): Patch = {
    (for {
      (replace, spaces) <- getReplaceAndSpaces(defn, body)
      explicitType <- getTypeAsSeenFromGlobal(name)
      filteredType <- filterType(explicitType)
      //      _ = println(s"filteredType.prefixString = ${filteredType.prefix}")
    } yield Patch.addRight(replace, s"$spaces: ${filteredType.finalResultType}")).getOrElse(Patch.empty)
  }

  private def getTypeAsSeenFromGlobal(name: Term.Name)
                                     (implicit doc: SemanticDocument, unit: g.CompilationUnit): Option[g.Type] = {
    for {
      context <- CompilerService.getContext(name, g)
      finalType = context.tree.symbol.info
    } yield finalType.asInstanceOf[g.Type]
  }

  private def filterType(finalType: g.Type): Option[g.Type] = {
    finalType match {
      case f if f.isInstanceOf[g.ConstantType] =>
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

  def getAbsoluteType(semanticTree: SemanticType): Option[String] = {
    val prettyPrint = Pretty.pretty(semanticTree).render(250)
    if (prettyPrint.nonEmpty) Some(prettyPrint) else None
  }
}