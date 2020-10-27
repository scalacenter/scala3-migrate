package migrate.utils

import java.time.Instant

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.MILLISECONDS

private[migrate] object Timer {
  def timedMs[A](task: => A): (A, FiniteDuration) = {
    val start  = Instant.now()
    val result = task

    (result, toFiniteDuration(start, Instant.now()))
  }

  def toFiniteDuration(start: Instant, end: Instant): FiniteDuration =
    FiniteDuration(end.toEpochMilli - start.toEpochMilli, MILLISECONDS).toCoarsest
}
