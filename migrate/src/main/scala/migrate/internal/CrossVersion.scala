package migrate.internal

sealed trait CrossVersion {
  override def toString: String = this match {
    case CrossVersion.Binary(prefix: String, suffix: String)      => s"Binary($prefix, $suffix)"
    case CrossVersion.Disabled                                    => "Disabled()"
    case CrossVersion.Constant(value: String)                     => s"Constant($value)"
    case CrossVersion.Patch                                       => "Patch()"
    case CrossVersion.Full(prefix: String, suffix: String)        => s"Full($prefix, $suffix)"
    case CrossVersion.For3Use2_13(prefix: String, suffix: String) => s"For3Use2_13($prefix, $suffix)"
    case CrossVersion.For2_13Use3(prefix: String, suffix: String) => s"For3Use2_13($prefix, $suffix)"
  }

  def prefix: String = ""
  def suffix: String = ""
}

object CrossVersion {
  case class Binary(override val prefix: String, override val suffix: String)      extends CrossVersion
  case object Disabled                                                             extends CrossVersion
  case class Constant(value: String)                                               extends CrossVersion
  case object Patch                                                                extends CrossVersion
  case class Full(override val prefix: String, override val suffix: String)        extends CrossVersion
  case class For3Use2_13(override val prefix: String, override val suffix: String) extends CrossVersion
  case class For2_13Use3(override val prefix: String, override val suffix: String) extends CrossVersion

  def apply(value: String): CrossVersion =
    value match {
      case "Disabled()"                     => Disabled
      case s"Binary($prefix, $suffix)"      => Binary(prefix, suffix)
      case s"Constant($value)"              => Constant(value)
      case "Patch()"                        => Patch
      case s"Full($prefix, $suffix)"        => Full(prefix, suffix)
      case s"For3Use2_13($prefix, $suffix)" => For3Use2_13(prefix, suffix)
      case s"For3Use2_13($prefix, $suffix)" => For2_13Use3(prefix, suffix)
      case _                                => throw new IllegalArgumentException(value)
    }
}
