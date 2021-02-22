/*
rules = [MigrationRule]
*/
package cats
package data

abstract private[data] class NestedFunctorFilter[F[_], G[_]] extends FunctorFilter[Nested[F, G, *]] {
  implicit val F: Functor[F]

  implicit val G: FunctorFilter[G]

  def functor: Functor[Nested[F, G, *]] = Nested.catsDataFunctorForNested(F, G.functor)

  def mapFilter[A, B](fa: Nested[F, G, A])(f: (A) => Option[B]): Nested[F, G, B] =
    Nested[F, G, B](F.map(fa.value)(G.mapFilter(_)(f)))

  override def collect[A, B](fa: Nested[F, G, A])(f: PartialFunction[A, B]): Nested[F, G, B] =
    Nested[F, G, B](F.map(fa.value)(G.collect(_)(f)))

  override def flattenOption[A](fa: Nested[F, G, Option[A]]): Nested[F, G, A] =
    Nested[F, G, A](F.map(fa.value)(G.flattenOption))

  override def filter[A](fa: Nested[F, G, A])(f: (A) => Boolean): Nested[F, G, A] =
    Nested[F, G, A](F.map(fa.value)(G.filter(_)(f)))
}

final case class IorT2[F[_], A, B](value: F[Ior[A, B]]) {
  
  def traverse[G[_], D](f: B => G[D])(implicit traverseF: Traverse[F], applicativeG: Applicative[G]): G[IorT[F, A, D]] =
    applicativeG.map(traverseF.traverse(value)(ior => Traverse[Ior[A, *]].traverse(ior)(f)))(IorT.apply)

}