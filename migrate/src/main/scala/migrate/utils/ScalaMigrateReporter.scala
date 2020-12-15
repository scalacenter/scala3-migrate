package migrate.utils

import compiler.interfaces.Logger

case object ScalaMigrateLogger extends Logger {
  override def error(log: String): Unit = scribe.error(log)
}
