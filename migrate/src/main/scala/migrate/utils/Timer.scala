package migrate.utils

import scala.util.control.NonFatal
import scala.util.{Success, Failure, Try}
import scribe._
import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import scala.annotation.compileTimeOnly

private[migrate] object Timer {
  def timedMs[A](task: => A): (A, Long) ={
    val start = System.currentTimeMillis()
    val result = task
    (result, System.currentTimeMillis() - start)
  }
}
