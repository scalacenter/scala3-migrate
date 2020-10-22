package migrate

import scala.meta.Lit

import org.typelevel.paiges.Doc
import scalafix.internal.v1.Types
import scalafix.v1.NoType
import scalafix.v1._

object Pretty {

  import scalafix.internal.util.DocConstants._

  def pretty(constant: Constant): Doc = constant match {
    case UnitConstant           => `()`
    case BooleanConstant(value) => Doc.str(value)
    case ByteConstant(value)    => Doc.text(Lit.Byte(value).syntax)
    case ShortConstant(value)   => Doc.text(Lit.Short(value).syntax)
    case CharConstant(value)    => Doc.text(Lit.Char(value).syntax)
    case IntConstant(value)     => Doc.str(value)
    case LongConstant(value)    => Doc.text(Lit.Long(value).syntax)
    case FloatConstant(value)   => Doc.text(Lit.Float(value).syntax)
    case DoubleConstant(value)  => Doc.text(Lit.Double(value).syntax)
    case StringConstant(value)  => Doc.text(Lit.String(value).syntax)
    case NullConstant           => `null`
  }

  def pretty(prefix: SemanticType, sym: Symbol): Doc = {
    def removeEmptyPackage(symbol: Symbol): String = {
      val stringSymb   = sym.normalized.value
      val emptyPackage = "_empty_."
      if (stringSymb.startsWith(emptyPackage)) stringSymb.drop(emptyPackage.length) else stringSymb
    }

    prefix match {
      case NoType if !isLocalToBlock(sym) =>
        Doc.text(removeEmptyPackage(sym).dropRight(1))
      case _ => Doc.text(sym.displayName)
    }
  }

  def pretty(symbol: Symbol): Doc =
    Doc.text(symbol.displayName)

  def pretty(tpe: SemanticType): Doc = {
    def prefix(tpe: SemanticType): Doc =
      tpe match {
        case NoType => Doc.empty
        case t: TypeRef =>
          val pre: Doc = t.prefix match {
            case _ if t.symbol.isLocal => Doc.empty
            case _: SingleType | _: ThisType | _: SuperType =>
              prefix(t.prefix) + `.`
            case NoType =>
              Doc.empty
            case _ =>
              prefix(t.prefix) + Doc.char('#')
          }
          val symbol = pretty(t.prefix, t.symbol)
          val targs  = t.typeArguments.map(normal)
          targs match {
            case Nil                              => pre + symbol
            case docs if docs.contains(Doc.empty) => Doc.empty
            case docs =>
              pre + symbol + `[` + Doc.intercalate(Doc.comma + Doc.space, docs) + `]`
          }
        case t: SingleType =>
          if (t.prefix.isEmpty) pretty(t.symbol)
          else prefix(t.prefix) + `.` + pretty(t.symbol)
        case t: ThisType =>
          if (t.symbol.isNone) pretty(t.symbol)
          else pretty(t.symbol) + `.` + `this`
        case t: SuperType =>
          val pre =
            if (t.prefix.isEmpty) Doc.empty
            else prefix(t.prefix) + `.`
          val sym =
            if (t.symbol.isNone) Doc.empty
            else `[` + pretty(t.symbol) + `]`
          pre + `super` + sym
        case t: ConstantType =>
          pretty(t.constant)
        case t: IntersectionType =>
          Doc.empty
        case t: UnionType =>
          Doc.empty
        case t: WithType       => Doc.empty
        case t: StructuralType => Doc.empty
        case t: AnnotatedType =>
          val annotations = t.annotations.filter(_.tpe.nonEmpty)
          val annots =
            if (annotations.isEmpty) {
              Doc.empty
            } else {
              Doc.space + Doc.intercalate(Doc.space, annotations.map(pretty))
            }
          normal(t.tpe) + annots
        case t: ExistentialType => Doc.empty // dotty doesn't have ExistentialType
        case t: UniversalType =>
          val tparams = Doc
            .intercalate(Doc.comma, t.typeParameters.map(pretty))
            .tightBracketBy(`[`, `]` + Doc.space + Doc.text("=>") + Doc.space)
          normal(t.tpe) + tparams
        case t: ByNameType =>
          Doc.text("=>") + Doc.space + pretty(t.tpe)
        case t: RepeatedType =>
          `*` + Doc.space + pretty(t.tpe)
      }

    def normal(tpe: SemanticType): Doc = tpe match {
      case _: SingleType | _: ThisType | _: SuperType =>
        prefix(tpe) + `.` + `type`
      case _ =>
        prefix(tpe)
    }

    normal(tpe)
  }

  def isLocalToBlock(symbol: Symbol): Boolean =
    symbol.owner.value.contains("()")

  def pretty(annotation: Annotation): Doc =
    if (annotation.tpe.isEmpty) Doc.empty
    else Doc.text("@") + pretty(annotation.tpe)

  def pretty(info: SymbolInformation): Doc =
    Doc.text(info.toString)

  def prettyTermParameter(parameter: SymbolInformation): Doc =
    Doc.text(parameter.displayName) + Doc.text(":") + Doc.space +
      pretty(parameter.signature)

  def prettyTypeParameter(parameter: SymbolInformation): Doc =
    Doc.text(parameter.displayName) + pretty(parameter.signature)

  def prettyTypeParameters(typeParameters: List[SymbolInformation]): Doc =
    if (typeParameters.isEmpty) Doc.empty
    else {
      `[` +
        Doc.intercalate(Doc.comma, typeParameters.map(prettyTypeParameter)) +
        `]`
    }

  def pretty(signature: Signature): Doc = signature match {
    case NoSignature => Doc.empty
    case t: ClassSignature =>
      val tparams = prettyTypeParameters(t.typeParameters)
      val parents = `extends` + Doc.space +
        Doc.intercalate(Doc.space + `with` + Doc.space, t.parents.map(pretty))
      val body =
        if (t.self.isEmpty && t.declarations.isEmpty) Doc.empty
        else {
          val self =
            if (t.self.isEmpty) Doc.empty
            else Doc.text("self: ") + pretty(t.self) + Doc.text(" =>")
          val declarations =
            if (t.declarations.isEmpty) Doc.empty
            else
              Doc.text(s" +") + Doc.str(t.declarations.length) +
                Doc.text(" decls ")
          Doc.space + `{` + self + declarations + `}`
        }
      tparams + Doc.space + parents + body
    case t: MethodSignature =>
      val tparams = prettyTypeParameters(t.typeParameters)
      val params =
        if (t.parameterLists.isEmpty) Doc.empty
        else {
          val params = t.parameterLists.map { parameterList =>
            Doc.intercalate(Doc.comma, parameterList.map(prettyTermParameter))
          }
          val paramss = Doc.intercalate(`)` + `(`, params)

          `(` + paramss + `)`
        }
      val returnType = pretty(t.returnType)
      tparams + params + Doc.char(':') + Doc.space + returnType
    case t: TypeSignature =>
      val tparams = prettyTypeParameters(t.typeParameters)
      if (t.lowerBound == t.upperBound) {
        tparams + Doc.text(" = ") + pretty(t.lowerBound)
      } else {
        val lowerBound =
          if (t.lowerBound == Types.scalaNothing) Doc.empty
          else Doc.text(" >: ") + pretty(t.lowerBound)
        val upperBound =
          if (t.upperBound == Types.scalaAny) Doc.empty
          else if (t.upperBound == Types.javaObject) Doc.empty
          else Doc.text(" <: ") + pretty(t.upperBound)
        tparams + lowerBound + upperBound
      }
    case t: ValueSignature =>
      pretty(t.tpe)
  }

  def pretty(tree: SemanticTree): Doc = tree match {
    case NoTree =>
      Doc.empty
    case t: IdTree =>
      pretty(t.symbol)
    case t: SelectTree =>
      pretty(t.qualifier) + `.` + pretty(t.id)
    case t: ApplyTree =>
      Doc
        .intercalate(Doc.comma, t.arguments.map(pretty))
        .tightBracketBy(pretty(t.function) + `(`, `)`)
    case t: TypeApplyTree =>
      val targs = Doc.intercalate(Doc.comma, t.typeArguments.map(pretty))
      pretty(t.function) + `[` + targs + `]`
    case t: FunctionTree =>
      val paramNames =
        t.parameters.map(param => Doc.text(param.info.displayName))
      val params = `(` +
        Doc.intercalate(Doc.comma, paramNames) +
        `)` + Doc.space + Doc.text("=>")
      pretty(t.body).bracketBy(`{` + Doc.space + params, `}`)
    case t: LiteralTree =>
      pretty(t.constant)
    case t: MacroExpansionTree =>
      `(` + Doc.text("`after-expansion` : ") + pretty(t.tpe) + `)`
    case r: OriginalTree =>
      `*`
    case r: OriginalSubTree =>
      Doc.text("orig") + `(` + Doc.text(r.tree.syntax) + `)`
  }

}
