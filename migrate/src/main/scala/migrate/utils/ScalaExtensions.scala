package migrate.utils

import java.util.Optional

import scala.collection.BuildFrom
import scala.util.Failure
import scala.util.Success
import scala.util.Try

private[migrate] object ScalaExtensions {
  implicit class TraversableOnceTryExtension[A, M[X] <: IterableOnce[X]](val in: M[Try[A]]) extends AnyVal {
    def sequence(implicit bf: BuildFrom[M[Try[A]], A, M[A]]): Try[M[A]] = {
      val init = Try(bf.newBuilder(in))
      in.iterator
        .foldLeft(init) { (acc, cur) =>
          acc.flatMap { case builder =>
            cur.map(result => builder += result)
          }
        }
        .map(_.result())
    }
  }

  implicit class OptionExtension[A](val in: Option[A]) extends AnyVal {
    def asJava: Optional[A] = in match {
      case Some(a) => Optional.ofNullable(a)
      case _       => Optional.empty[A]
    }
    def toTry: Try[A] = in match {
      case Some(v) => Success(v)
      case None    => Failure(new Exception("Empty value"))
    }
    def toTry(e: => Throwable): Try[A] = in match {
      case Some(v) => Success(v)
      case None    => Failure(e)
    }
  }
}
