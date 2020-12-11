package cats
package data

abstract private[data] class NestedApplicativeError[F[_], G[_], E]
  extends ApplicativeError[Nested[F, G, *], E]
    with NestedApplicative[F, G] {
  def G: Applicative[G]
  def AEF: ApplicativeError[F, E]

  def FG: Applicative[λ[α => F[G[α]]]] = AEF.compose[G](G)

  def raiseError[A](e: E): Nested[F, G, A] = Nested(AEF.map(AEF.raiseError(e))(G.pure[A]))

  def handleErrorWith[A](fa: Nested[F, G, A])(f: E => Nested[F, G, A]): Nested[F, G, A] =
    Nested(AEF.handleErrorWith(fa.value)(e => f(e).value))

}