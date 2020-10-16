package utils

import scala.collection.BuildFrom

object ScalaExtensions {
  implicit class TraversableOnceOptionExtension[A, M[X] <: IterableOnce[X]](val in: M[Option[A]]) extends AnyVal {
    def sequence(implicit bf: BuildFrom[M[Option[A]], A, M[A]]): Option[M[A]] = {
      val init = Option(bf(in))
      in.iterator.foldLeft(init) { (acc, cur) =>
        acc.flatMap { results =>
          cur.map { result => results += result }
        }
      }.map(_.result())
    }
  }
}
