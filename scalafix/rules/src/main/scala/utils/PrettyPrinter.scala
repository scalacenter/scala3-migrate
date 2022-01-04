package scala.meta.internal.pc

import scala.collection.mutable.ListBuffer
import scala.tools.nsc.interactive.Global
import scala.{meta => m}

import utils.ScalaExtensions.TraversableOnceOptionExtension

class PrettyPrinter[G <: Global](val g: G) {
  import g._

  def print(gtype: g.Type, context: g.Context): Option[g.Type] = {

    def loop(t: Type): Option[Type] =
      if (filterType(t)) None
      else {
        t match {
          case TypeRef(pre, sym, args) =>
            if (sym.isExistentialSkolem) None
            else {
              val lookUp  = context.lookupSymbol(sym.name, _ => true)
              val argsOpt = args.map(loop).sequence
              if (isTheSameSymbol(sym, lookUp, pre)) {
                argsOpt.map(a => TypeRef(g.NoPrefix, sym, a))
              } else argsOpt.map(a => TypeRef(loop(pre).get, sym, a))
            }
          case SingleType(pre, sym) =>
            val lookUp = context.lookupSymbol(sym.name, _ => true)
            if (isTheSameSymbol(sym, lookUp, pre))
              Some(SingleType(NoPrefix, sym))
            else Some(SingleType(loop(pre).get, sym))

          case ThisType(sym) =>
            Some(new PrettyType(lookUpName(sym, context)))
          case ConstantType(Constant(_: TermSymbol)) => Some(t)
          case ConstantType(Constant(_: Type))       => Some(t)
          case SuperType(_, _)                       => Some(t)
          case RefinedType(parents, decls) =>
            val parentOp = parents.map(loop).sequence
            parentOp.map(p => RefinedType(p, decls))
          case AnnotatedType(_, _) => Some(t)
          case ExistentialType(quantified, underlying) =>
            scala.util
              .Try(ExistentialType(quantified.map(sym => sym.setInfo(loop(sym.info).get)), loop(underlying).get))
              .toOption
          case PolyType(typeParams, resultType) =>
            scala.util
              .Try(resultType.map(t => loop(t).get))
              .toOption match {
              // [x] => F[x] is not printable in the code, we need to use just `F`
              case Some(TypeRef(_, sym, args)) if typeParams == args.map(_.typeSymbol) =>
                Some(new PrettyType(sym.name.toString()))
              case Some(otherType) =>
                Some(PolyType(typeParams, otherType))
              case None => None
            }
          case NullaryMethodType(resultType) =>
            loop(resultType)
          case TypeBounds(lo, hi) =>
            (loop(lo), loop(hi)) match {
              case (Some(lo), Some(hi)) => Some(TypeBounds(lo, hi))
              case _                    => None
            }
          case MethodType(_, _) => Some(t)
          case ErrorType        => Some(definitions.AnyTpe)
          case t                => Some(t)
        }
      }

    gtype match {
      case _ if gtype.toString() == "_" => None // for topLevel WildCard.
      case ThisType(_)                  => Some(gtype)
      case _                            => loop(gtype)
    }
  }

  def print(gsymbol: g.Symbol, context: g.Context): Option[String] =
    if (isPrivateMaybeWithin(gsymbol)) None
    else if (gsymbol.name.startsWith("evidence$")) None
    else if (gsymbol.isLocalToBlock)
      Some(gsymbol.name.toString())
    else if (gsymbol.isStatic) Some(lookUpName(gsymbol, context))
    else None

  private def lookUpName(sym: g.Symbol, context: g.Context): String = {
    // first get all owners
    val owners = getOwnersFor(sym)
    val necessaryOwners = owners.iterator.takeWhile { case sym =>
      val lookUp = context.lookupSymbol(sym.name.toTermName, _ => true)
      !isTheSameSymbol(sym, lookUp)
    }.toSeq

    val size = necessaryOwners.size
    necessaryOwners match {
      case Nil => sym.name.toTermName.toString
      case _ if size < owners.size - 1 =>
        val names = owners.take(size + 1).reverse.map(s => m.Term.Name(s.nameSyntax))
        val ref = names.tail.foldLeft(names.head: m.Term.Ref) { case (qual, name) =>
          m.Term.Select(qual, name)
        }
        ref.syntax
      case _ if size >= owners.size - 1 =>
        val top = owners.last
        val to  = context.lookupSymbol(top.name.toTermName, _ => true)
        val inScope = to match {
          case LookupSucceeded(qual, _) => !qual.isEmpty
          case _                        => false
        }
        if (inScope && top.isStatic) {
          s"${top.owner.nameSyntax}.${sym.fullNameSyntax}"
        } else sym.fullNameSyntax
    }
  }

  private def isTheSameSymbol(sym: g.Symbol, nameLookup: NameLookup, prefix: Type = NoPrefix): Boolean =
    nameLookup match {
      case LookupSucceeded(qual, symbol) =>
        symbol.isKindaTheSameAs(sym) && {
          prefix == NoPrefix ||
          prefix.isInstanceOf[PrettyType] ||
          qual.tpe.computeMemberType(symbol) <:<
            prefix.computeMemberType(sym)
        }
      case _ => false
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
      // Todo: add a special case for structural type: remove implicit and replace lazy val by a def
      case f if f.isStructuralRefinement && (f.toString().contains("implicit") || f.toString().contains("lazy val")) =>
        true
      case _ => false
    }

  def topPackage(s: Symbol): Symbol = {
    val owner = s.owner
    if (s.isRoot || s.isRootPackage || s == NoSymbol || s.owner.isEffectiveRoot || s == owner) {
      s
    } else {
      topPackage(owner)
    }
  }
  def getOwnersFor(symbol: Symbol): Seq[Symbol] = {
    def loop(symbol: Symbol, b: ListBuffer[Symbol]): ListBuffer[Symbol] =
      symbol match {
        case _
            if symbol.isRoot || symbol.isRootPackage || symbol == NoSymbol || symbol.owner.isEffectiveRoot || symbol == symbol.owner =>
          b += symbol
        case _ =>
          b += symbol
          loop(symbol.owner, b)
      }
    loop(symbol, ListBuffer.empty[Symbol]).toSeq
  }

  implicit class XtensionSymbol(sym: Symbol) {
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

    def isKindaTheSameAs(other: Symbol): Boolean =
      if (sym.fullName == other.fullName) true
      else if (other == NoSymbol) sym == NoSymbol
      else if (sym == NoSymbol) false
      else if (sym.hasPackageFlag) {
        // NOTE(olafur) hacky workaround for comparing module symbol with package symbol
        other.fullName == sym.fullName
      } else {
        sym.dealiased == other.dealiased ||
        sym.companion == other.dealiased
      }

    def dealiasedSingleType: Symbol =
      if (sym.isValue) {
        sym.info.resultType match {
          case SingleType(_, dealias) => dealias
          case _                      => sym
        }
      } else {
        sym
      }

    def dealiased: Symbol =
      if (sym.isAliasType) sym.info.dealias.typeSymbol
      else if (sym.isValue) dealiasedSingleType
      else sym
  }

  class PrettyType(override val prefixString: String, override val safeToString: String) extends Type {
    def this(string: String) =
      this(string + ".", string)
  }
}
