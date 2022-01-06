package migrate.internal

import compiler.interfaces.Logger

case object ScalaMigrateLogger extends Logger {

  private val logger = scribe
    .Logger()
    .orphan()
    .replace()
    .clearHandlers()
    .withHandler(formatter = scribe.format.Formatter.simple)
    .replace()
  override def error(log: String): Unit = logger.error(log)
}
