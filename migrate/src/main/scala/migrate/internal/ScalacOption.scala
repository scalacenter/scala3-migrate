package migrate.internal

sealed trait MigratedScalacOption

object MigratedScalacOption {
  case class Valid(value: String)                              extends MigratedScalacOption
  case class Renamed(scala2Value: String, scala3Value: String) extends MigratedScalacOption
  case class Removed(value: String)                            extends MigratedScalacOption
  case class Unknown(value: String)                            extends MigratedScalacOption
  case object Ignored                                          extends MigratedScalacOption
}
