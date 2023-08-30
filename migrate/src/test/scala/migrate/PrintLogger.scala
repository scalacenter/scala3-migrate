package migrate

import scala.Console._

import migrate.interfaces.Logger

object PrintLogger extends Logger {

  override def info(msg: String): Unit = println(s"[info] $msg")

  override def warn(msg: String): Unit = println(s"$YELLOW[warn] $msg$RESET")

  override def error(msg: String): Unit = println(s"$RED[error] $msg$RESET")
}
