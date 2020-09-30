package utils

import java.util.Optional

import scala.collection.BuildFrom
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object ScalaExtensions {
  implicit class TraversableOnceTryExtension[A, M[X] <: IterableOnce[X]](val in: M[Try[A]]) extends AnyVal {
    def sequence(implicit bf: BuildFrom[M[Try[A]], A, M[A]]): Try[M[A]] = {
      val init = Try(bf(in) -> Seq.empty[Throwable])
      in.iterator.foldLeft(init) { (acc, cur) =>
        acc.flatMap { case (results, errors) =>
          cur.map { result => (results += result, errors) }
            .recover { case NonFatal(error) => (results, error +: errors) }
        }
      }.flatMap {case (results, errors) =>
        val errorsLength = errors.length
        if (errorsLength >= 1) Failure(errors.head)
        else Success(results.result())
      }
    }
  }

  implicit class OptionExtension[A](val in: Option[A]) extends AnyVal {
    def asJava: Optional[A] = in match {
      case Some(a) => Optional.ofNullable(a)
      case _ => Optional.empty[A]
    }
    def toTry: Try[A] = in match {
      case Some(v) => Success(v)
      case None => Failure(new Exception("Empty value"))
    }
  }

  implicit class OptionalExtension[A](val in: Optional[A]) extends AnyVal {
    def asScala: Option[A] = if (in.isPresent) Some(in.get()) else None
  }
}
