package migrate

class ScalaMigrateLogger(underlying: sbt.Logger) extends migrate.interfaces.Logger {

  override def info(msg: String): Unit = underlying.info(msg)

  override def warn(msg: String): Unit = underlying.warn(msg)

  override def error(msg: String): Unit = underlying.error(msg)
}
