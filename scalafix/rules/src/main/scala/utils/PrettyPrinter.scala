package scala.meta.internal.pc

import scala.tools.nsc.interactive.Global

import utils.ScalaExtensions.TraversableOnceOptionExtension

class PrettyPrinter[G <: Global](val g: G) {
  import g._

  def print(gtype: g.Type, context: g.Context): Option[g.Type] = {

    def loop(t: Type): Option[Type] =
      if (filterType(t)) None
      else {
        t match {
          case TypeRef(pre, sym, args) => {
            val top = topPackage(pre.typeSymbol)
            val to  = context.lookupSymbol(top.name.toTermName, _ => true)
            val inScope = to match {
              case LookupSucceeded(qual, _) => !qual.isEmpty
              case _                        => false
            }
            val argsOpt = args.map(loop).sequence
            if (inScope && top.isStatic) {
              argsOpt.map(a =>
                TypeRef(new PrettyType(s"${top.owner.nameSyntax}.${pre.termSymbol.fullNameSyntax}"), sym, a)
              )
            } else argsOpt.map(a => TypeRef(pre, sym, a))
          }
          case SingleType(pre, sym)                    => Some(t)
          case ThisType(sym)                           => Some(t)
          case ConstantType(Constant(sym: TermSymbol)) => Some(t)
          case ConstantType(Constant(tpe: Type))       => Some(t)
          case SuperType(thistpe, supertpe)            => Some(t)
          case RefinedType(parents, decls)             => Some(t)
          case AnnotatedType(annotations, underlying)  => Some(t)
          case ExistentialType(quantified, underlying) => Some(t)
          case PolyType(tparams, resultType)           => Some(t)
          case NullaryMethodType(resultType)           => Some(t)
          case TypeBounds(lo, hi)                      => Some(t)
          case MethodType(params, resultType)          => Some(t)
          case ErrorType                               => Some(definitions.AnyTpe)
          case t                                       => Some(t)
        }
      }

    gtype match {
      case ThisType(_) => Some(gtype)
      case _           => loop(gtype)
    }
  }

  def print(gsymbol: g.Symbol, context: g.Context): Option[String] = {
    val top = topPackage(gsymbol)
    val to  = context.lookupSymbol(top.name.toTermName, _ => true)
    val inScope = to match {
      case LookupSucceeded(qual, _) => !qual.isEmpty
      case _                        => false
    }

    if (isPrivateMaybeWithin(gsymbol)) None
    else if (gsymbol.name.startsWith("evidence$")) None
    else if (gsymbol.isLocalToBlock)
      Some(gsymbol.name.toString())
    else if (inScope && top.isStatic) {
      Some(s"${top.owner.nameSyntax}.${gsymbol.fullName}")
    } else if (gsymbol.isStatic) Some(gsymbol.fullName)
    else None
  }

  private def isPrivateMaybeWithin(gsymbol: g.Symbol): Boolean =
    gsymbol.isPrivate || (gsymbol.hasAccessBoundary && !gsymbol.isProtected)
  private def filterType(finalType: g.Type): Boolean =
    finalType match {
      case g.ErrorType => true
      case g.ConstantType(_) =>
        true // don't annotate ConstantTypes
      case f if f.toString().contains(".super.") =>
        true // remove super types: we don't infer them
      case f if f.toString().contains("#") && f.toString().contains(".type") =>
        true // don't annotate types that look like fix.WidenSingleType#strings.type
      //Todo: add a special case for structural type: remove implicit and replace lazy val by a def
      case f if f.isStructuralRefinement && (f.toString().contains("implicit") || f.toString().contains("lazy val")) =>
        true
      // PolyTypes need a better toString
      case g.PolyType(_, _) => true
      case _                => false
    }

  def topPackage(s: Symbol): Symbol = {
    val owner = s.owner
    if (s.isRoot || s.isRootPackage || s == NoSymbol || s.owner.isEffectiveRoot || s == owner) {
      s
    } else {
      topPackage(owner)
    }
  }

  implicit class XtensionSymbolMetals(sym: Symbol) {
    def nameSyntax: String =
      if (sym.isEmptyPackage || sym.isEmptyPackageClass) "_empty_"
      else if (sym.isRootPackage || sym.isRoot) "_root_"
      else sym.nameString

    def fullNameSyntax: String = {
      val out = new java.lang.StringBuilder

      def loop(s: Symbol): Unit =
        if (s.isRoot || s.isRootPackage || s == NoSymbol || s.owner.isEffectiveRoot) {
          out.append(Identifier(s.nameSyntax))
        } else {
          loop(s.effectiveOwner.enclClass)
          out.append('.').append(Identifier(s.name))
        }

      loop(sym)
      out.toString
    }
  }

  class PrettyType(override val prefixString: String, override val safeToString: String) extends Type {
    def this(string: String) =
      this(string + ".", string)
  }
}
