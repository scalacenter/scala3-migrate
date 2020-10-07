package migrate.utils

import java.time.{Instant}

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}
import scala.language.experimental.macros

private[migrate] object Timer {
  def timedMs[A](task: => A): (A, FiniteDuration) ={
    val start =  Instant.now()
    val result = task

    (result, toFiniteDuration(start, Instant.now()))
  }

  def toFiniteDuration(start: Instant, end: Instant): FiniteDuration =
    FiniteDuration(end.toEpochMilli - start.toEpochMilli, MILLISECONDS).toCoarsest
}
