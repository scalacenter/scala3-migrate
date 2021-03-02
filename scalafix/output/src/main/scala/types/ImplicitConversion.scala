package cats
package data

import cats.syntax.option._

final case class IorT[F[_], A, B](value: F[Ior[A, B]]) {

  final def fromOptionF[F[_], E, A](foption: F[Option[A]], ifNone: => E)(implicit F: Functor[F]): IorT[F, E, A] =
    IorT.apply[F, E, A](F.map[Option[A], Ior[E,A]](foption)(catsSyntaxOption(_).toRightIor[E](ifNone)))
}