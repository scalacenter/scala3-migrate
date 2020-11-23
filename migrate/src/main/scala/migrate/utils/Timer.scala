package migrate.utils

import java.time.Instant

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.MILLISECONDS

private[migrate] object Timer {
  def timeAndLog[A](task: => A)(log: (FiniteDuration, A) => Unit): A = {
    val start  = Instant.now()
    val result = task

    val duration = toFiniteDuration(start, Instant.now())
    log(duration, result)
    result
  }

  def toFiniteDuration(start: Instant, end: Instant): FiniteDuration =
    FiniteDuration(end.toEpochMilli - start.toEpochMilli, MILLISECONDS).toCoarsest

}
