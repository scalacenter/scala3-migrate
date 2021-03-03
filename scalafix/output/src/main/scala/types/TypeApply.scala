import zio.Cause

import scala.annotation.tailrec

object TypeApply {

  implicit class CauseExtension[+E](c: Cause[E]) {
    private def flatten(c: Cause[_]): List[Set[Cause[_]]] = {
      def step(c: Cause[_]): (Set[Cause[_]], List[Cause[_]]) = ???
      @tailrec
      def loop(causes: List[Cause[_]], flattened: List[Set[Cause[_]]]): List[Set[Cause[_]]] = {
        val (parallel, sequential) = causes.foldLeft[(Set[zio.Cause[_]], List[zio.Cause[_]])]((Set.empty[Cause[_]], List.empty[Cause[_]])) {
          case ((parallel, sequential), cause) =>
            val (set, seq) = step(cause)
            (parallel ++ set, sequential ++ seq)
        }
        val updated: List[Set[Cause[_]]] = if (parallel.nonEmpty) parallel :: flattened else flattened
        if (sequential.isEmpty) updated.reverse
        else loop(sequential, updated)
      }

      loop(List(c), List.empty[Nothing])
    }
  }

}