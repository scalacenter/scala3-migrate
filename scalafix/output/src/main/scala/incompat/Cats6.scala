package cats.instances

import cats.{Always, Applicative, Eval, FlatMap, Foldable, Monoid, MonoidK, Order, Show, Traverse, TraverseFilter}
import cats.kernel._

import scala.annotation.tailrec
import scala.collection.immutable.SortedMap
import cats.Align
import cats.Functor
import cats.data.Ior

trait SortedMapInstances extends SortedMapInstances2 {

  @deprecated("Use cats.kernel.instances.sortedMap.catsKernelStdHashForSortedMap", "2.0.0-RC2")
  def catsStdHashForSortedMap[K: Hash: Order, V: Hash]: Hash[SortedMap[K, V]] =
    cats.kernel.instances.sortedMap.catsKernelStdHashForSortedMap[K, V]

  @deprecated("Use cats.kernel.instances.sortedMap.catsKernelStdCommutativeMonoidForSortedMap", "2.0.0-RC2")
  def catsStdCommutativeMonoidForSortedMap[K: Order, V: CommutativeSemigroup]: cats.kernel.CommutativeMonoid[collection.immutable.SortedMap[K,V]] =
    cats.kernel.instances.sortedMap.catsKernelStdCommutativeMonoidForSortedMap[K, V]

  implicit def catsStdShowForSortedMap[A: Order, B](implicit showA: Show[A], showB: Show[B]): Show[SortedMap[A, B]] =
    new Show[SortedMap[A, B]] {
      def show(m: SortedMap[A, B]): String =
        m.iterator
          .map[String] { case (a, b) => showA.show(a) + " -> " + showB.show(b) }
          .mkString("SortedMap(", ", ", ")")
    }

  // scalastyle:off method.length
  implicit def catsStdInstancesForSortedMap[K: Order]
  : Traverse[SortedMap[K, *]] with FlatMap[SortedMap[K, *]] with Align[SortedMap[K, *]] =
    new Traverse[SortedMap[K, *]] with FlatMap[SortedMap[K, *]] with Align[SortedMap[K, *]] {

      implicit val orderingK: Ordering[K] = Order[K].toOrdering

      def traverse[G[_], A, B](fa: SortedMap[K, A])(f: A => G[B])(implicit G: Applicative[G]): G[SortedMap[K, B]] = {
        val gba: Eval[G[SortedMap[K, B]]] = Always.apply[G[SortedMap[K,B]]](G.pure[SortedMap[K,B]](SortedMap.empty[K, Nothing](Order[K].toOrdering)))
        Foldable
          .iterateRight[(K, A), G[SortedMap[K,B]]](fa, gba) { (kv, lbuf) =>
            G.map2Eval[B, SortedMap[K,B], SortedMap[K,B]](f(kv._2), lbuf)({ (b, buf) =>
              buf + (scala.Predef.ArrowAssoc(kv._1) -> b)
            })
          }
          .value
      }

      def flatMap[A, B](fa: SortedMap[K, A])(f: A => SortedMap[K, B]): SortedMap[K, B] =
        fa.flatMap[K, B] { case (k, a) => f(a).get(k).map[(K, B)]((k, _)) }

      override def map[A, B](fa: SortedMap[K, A])(f: A => B): SortedMap[K, B] =
        fa.map[K, B] { case (k, a) => (k, f(a)) }

      override def map2Eval[A, B, Z](fa: SortedMap[K, A],
                                     fb: Eval[SortedMap[K, B]])(f: (A, B) => Z): Eval[SortedMap[K, Z]] =
        if (fa.isEmpty) Eval.now[SortedMap[K,Nothing]](SortedMap.empty[K, Nothing](Order[K].toOrdering)) // no need to evaluate fb
        else fb.map[SortedMap[K,Z]](fb => map2[A, B, Z](fa, fb)(f))

      override def ap2[A, B, Z](f: SortedMap[K, (A, B) => Z])(fa: SortedMap[K, A],
                                                              fb: SortedMap[K, B]): SortedMap[K, Z] =
        f.flatMap[K, Z] {
          case (k, f) =>
            for { a <- fa.get(k); b <- fb.get(k) } yield (k, f(a, b))
        }

      def foldLeft[A, B](fa: SortedMap[K, A], b: B)(f: (B, A) => B): B =
        fa.foldLeft[B](b) { case (x, (k, a)) => f(x, a) }

      def foldRight[A, B](fa: SortedMap[K, A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
        Foldable.iterateRight[A, B](fa.values, lb)(f)

      def tailRecM[A, B](a: A)(f: A => SortedMap[K, Either[A, B]]): SortedMap[K, B] = {
        val bldr: collection.mutable.Builder[(K, B),SortedMap[K,B]] = SortedMap.newBuilder[K, B](Order[K].toOrdering)

        @tailrec def descend(k: K, either: Either[A, B]): Unit =
          either match {
            case Left(a) =>
              f(a).get(k) match {
                case Some(x) => descend(k, x)
                case None    => ()
              }
            case Right(b) =>
              bldr += ((k, b))
              ()
          }

        f(a).foreach[Unit] { case (k, a) => descend(k, a) }
        bldr.result
      }

      override def size[A](fa: SortedMap[K, A]): Long = fa.size.toLong

      override def get[A](fa: SortedMap[K, A])(idx: Long): Option[A] =
        if (idx < 0L || Int.MaxValue < idx) None
        else {
          val n: Int = idx.toInt
          if (n >= fa.size) None
          else Some.apply[A](fa.valuesIterator.drop(n).next)
        }

      override def isEmpty[A](fa: SortedMap[K, A]): Boolean = fa.isEmpty

      override def fold[A](fa: SortedMap[K, A])(implicit A: Monoid[A]): A =
        A.combineAll(fa.values)

      override def toList[A](fa: SortedMap[K, A]): List[A] = fa.values.toList

      override def collectFirst[A, B](fa: SortedMap[K, A])(pf: PartialFunction[A, B]): Option[B] =
        fa.collectFirst[B](new PartialFunction[(K, A), B] {
          override def isDefinedAt(x: (K, A)): Boolean = pf.isDefinedAt(x._2)
          override def apply(v1: (K, A)): B = pf(v1._2)
        })

      override def collectFirstSome[A, B](fa: SortedMap[K, A])(f: A => Option[B]): Option[B] =
        collectFirst[A, B](fa)(Function.unlift[A, B](f))

      def functor: Functor[SortedMap[K, *]] = this

      def align[A, B](fa: SortedMap[K, A], fb: SortedMap[K, B]): SortedMap[K, Ior[A, B]] =
        alignWith[A, B, Ior[A,B]](fa, fb)(identity[Ior[A,B]])

      override def alignWith[A, B, C](fa: SortedMap[K, A], fb: SortedMap[K, B])(f: Ior[A, B] => C): SortedMap[K, C] = {
        val keys: collection.immutable.SortedSet[K] = fa.keySet ++ fb.keySet
        val builder: collection.mutable.Builder[(K, C),SortedMap[K,C]] = SortedMap.newBuilder[K, C]
        builder.sizeHint(keys.size)
        keys
          .foldLeft[collection.mutable.Builder[(K, C),SortedMap[K,C]]](builder) { (builder, k) =>
            (fa.get(k), fb.get(k)) match {
              case (Some(a), Some(b)) => builder += scala.Predef.ArrowAssoc(k) -> f(Ior.both[A, B](a, b))
              case (Some(a), None)    => builder += scala.Predef.ArrowAssoc(k) -> f(Ior.left[A, Nothing](a))
              case (None, Some(b))    => builder += scala.Predef.ArrowAssoc(k) -> f(Ior.right[Nothing, B](b))
              case (None, None)       => ??? // should not happen
            }
          }
          .result()
      }
    }

}

private[instances] trait SortedMapInstances1 {
  @deprecated("Use cats.kernel.instances.sortedMap.catsKernelStdEqForSortedMap", "2.0.0-RC2")
  def catsStdEqForSortedMap[K: Order, V: Eq]: Eq[SortedMap[K, V]] =
    new SortedMapEq[K, V]
}

private[instances] trait SortedMapInstances2 extends SortedMapInstances1 {
  @deprecated("Use cats.kernel.instances.sortedMap.catsKernelStdMonoidForSortedMap", "2.0.0-RC2")
  def catsStdMonoidForSortedMap[K: Order, V: Semigroup]: Monoid[SortedMap[K, V]] =
    new SortedMapMonoid[K, V]
}

@deprecated("Use cats.kernel.instances.SortedMapHash", "2.0.0-RC2")
class SortedMapHash[K, V](implicit V: Hash[V], O: Order[K], K: Hash[K])
  extends SortedMapEq[K, V]
    with Hash[SortedMap[K, V]] {
  private[this] val underlying: Hash[SortedMap[K, V]] = new cats.kernel.instances.SortedMapHash[K, V]
  def hash(x: SortedMap[K, V]): Int = underlying.hash(x)
}

@deprecated("Use cats.kernel.instances.SortedMapEq", "2.0.0-RC2")
class SortedMapEq[K, V](implicit V: Eq[V], O: Order[K]) extends cats.kernel.instances.SortedMapEq[K, V]

@deprecated("Use cats.kernel.instances.SortedMapCommutativeMonoid", "2.0.0-RC2")
class SortedMapCommutativeMonoid[K, V](implicit V: CommutativeSemigroup[V], O: Order[K])
  extends SortedMapMonoid[K, V]
    with CommutativeMonoid[SortedMap[K, V]] {
  private[this] val underlying: CommutativeMonoid[SortedMap[K, V]] =
    new cats.kernel.instances.SortedMapCommutativeMonoid[K, V]
}

@deprecated("Use cats.kernel.instances.SortedMapMonoid", "2.0.0-RC2")
class SortedMapMonoid[K, V](implicit V: Semigroup[V], O: Order[K]) extends cats.kernel.instances.SortedMapMonoid[K, V]

private[instances] trait SortedMapInstancesBinCompat0 {
  implicit def catsStdTraverseFilterForSortedMap[K: Order]: TraverseFilter[SortedMap[K, *]] =
    new TraverseFilter[SortedMap[K, *]] {

      implicit val ordering: Ordering[K] = Order[K].toOrdering

      val traverse: Traverse[SortedMap[K, *]] = cats.instances.sortedMap.catsStdInstancesForSortedMap[K]

      override def traverseFilter[G[_], A, B](
                                               fa: SortedMap[K, A]
                                             )(f: A => G[Option[B]])(implicit G: Applicative[G]): G[SortedMap[K, B]] = {
        val gba: Eval[G[SortedMap[K, B]]] = Always.apply[G[SortedMap[K,B]]](G.pure[SortedMap[K,B]](SortedMap.empty[K, Nothing]))
        Foldable
          .iterateRight[(K, A), G[SortedMap[K,B]]](fa, gba) { (kv, lbuf) =>
            G.map2Eval[Option[B], SortedMap[K,B], SortedMap[K,B]](f(kv._2), lbuf)({ (ob, buf) =>
              ob.fold[SortedMap[K,B]](buf)(b => buf + (scala.Predef.ArrowAssoc(kv._1) -> b))
            })
          }
          .value
      }

      override def mapFilter[A, B](fa: SortedMap[K, A])(f: (A) => Option[B]): SortedMap[K, B] =
        fa.collect[K, B](scala.Function.unlift[(K, A), (K, B)](t => f(t._2).map[(K, B)](scala.Predef.ArrowAssoc(t._1) -> _)))

      override def collect[A, B](fa: SortedMap[K, A])(f: PartialFunction[A, B]): SortedMap[K, B] =
        fa.collect[K, B](scala.Function.unlift[(K, A), (K, B)](t => f.lift(t._2).map[(K, B)](scala.Predef.ArrowAssoc(t._1) -> _)))

      override def flattenOption[A](fa: SortedMap[K, Option[A]]): SortedMap[K, A] =
        fa.collect[K, A](scala.Function.unlift[(K, Option[A]), (K, A)](t => t._2.map[(K, A)](scala.Predef.ArrowAssoc(t._1) -> _)))

      override def filter[A](fa: SortedMap[K, A])(f: (A) => Boolean): SortedMap[K, A] =
        fa.filter { case (_, v) => f(v) }

      override def filterA[G[_], A](
                                     fa: SortedMap[K, A]
                                   )(f: (A) => G[Boolean])(implicit G: Applicative[G]): G[SortedMap[K, A]] =
        traverseFilter[G, A, A](fa)(a => G.map[Boolean, Option[A]](f(a))(if (_) Some.apply[A](a) else None))(G)
    }
}

private[instances] trait SortedMapInstancesBinCompat1 {
  implicit def catsStdMonoidKForSortedMap[K: Order]: MonoidK[SortedMap[K, *]] = new MonoidK[SortedMap[K, *]] {
    override def empty[A]: SortedMap[K, A] = SortedMap.empty[K, A](Order[K].toOrdering)

    override def combineK[A](x: SortedMap[K, A], y: SortedMap[K, A]): SortedMap[K, A] = x ++ y
  }
}

private[instances] trait SortedMapInstancesBinCompat2 extends cats.kernel.instances.SortedMapInstances