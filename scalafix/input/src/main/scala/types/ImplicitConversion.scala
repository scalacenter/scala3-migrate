/*
rules = [InferTypes, ExplicitImplicits]
*/
package cats
package data

import cats.syntax.option._

final case class IorT[F[_], A, B](value: F[Ior[A, B]]) {

  final def fromOptionF[F[_], E, A](foption: F[Option[A]], ifNone: => E)(implicit F: Functor[F]): IorT[F, E, A] =
    IorT(F.map(foption)(_.toRightIor(ifNone)))
}