package cats

private[cats] trait ComposedDistributive[F[_], G[_]] extends Distributive[λ[α => F[G[α]]]] with ComposedFunctor[F, G] {
  outer =>
  def F: Distributive[F]
  def G: Distributive[G]

  override def distribute[H[_]: Functor, A, B](ha: H[A])(f: A => F[G[B]]): F[G[H[B]]] =
    F.map[H[G[B]], G[H[B]]](F.distribute[H, A, G[B]](ha)(f))(G.cosequence[H, B](_))
}

private[cats] trait ComposedInvariant[F[_], G[_]] extends Invariant[λ[α => F[G[α]]]] { outer =>
  def F: Invariant[F]
  def G: Invariant[G]

  override def imap[A, B](fga: F[G[A]])(f: A => B)(g: B => A): F[G[B]] =
    F.imap[G[A], G[B]](fga)(ga => G.imap[A, B](ga)(f)(g))(gb => G.imap[B, A](gb)(g)(f))
}

private[cats] trait ComposedFunctor[F[_], G[_]] extends Functor[λ[α => F[G[α]]]] with ComposedInvariant[F, G] { outer =>
  def F: Functor[F]
  def G: Functor[G]

  override def map[A, B](fga: F[G[A]])(f: A => B): F[G[B]] =
    F.map[G[A], G[B]](fga)(ga => G.map[A, B](ga)(f))
}

private[cats] trait ComposedApply[F[_], G[_]] extends Apply[λ[α => F[G[α]]]] with ComposedFunctor[F, G] { outer =>
  def F: Apply[F]
  def G: Apply[G]

  override def ap[A, B](fgf: F[G[A => B]])(fga: F[G[A]]): F[G[B]] =
    F.ap[G[A], G[B]](F.map[G[A => B], G[A] => G[B]](fgf)(gf => G.ap[A, B](gf)(_)))(fga)

  override def product[A, B](fga: F[G[A]], fgb: F[G[B]]): F[G[(A, B)]] =
    F.map2[G[A], G[B], G[(A, B)]](fga, fgb)(G.product[A, B])
}

private[cats] trait ComposedApplicative[F[_], G[_]] extends Applicative[λ[α => F[G[α]]]] with ComposedApply[F, G] {
  outer =>
  def F: Applicative[F]
  def G: Applicative[G]

  override def pure[A](x: A): F[G[A]] = F.pure[G[A]](G.pure[A](x))
}

private[cats] trait ComposedSemigroupK[F[_], G[_]] extends SemigroupK[λ[α => F[G[α]]]] { outer =>
  def F: SemigroupK[F]

  override def combineK[A](x: F[G[A]], y: F[G[A]]): F[G[A]] = F.combineK[G[A]](x, y)
}

private[cats] trait ComposedMonoidK[F[_], G[_]] extends MonoidK[λ[α => F[G[α]]]] with ComposedSemigroupK[F, G] { outer =>
  def F: MonoidK[F]

  override def empty[A]: F[G[A]] = F.empty[G[A]]
}

private[cats] trait ComposedAlternative[F[_], G[_]]
  extends Alternative[λ[α => F[G[α]]]]
    with ComposedApplicative[F, G]
    with ComposedMonoidK[F, G] { outer =>
  def F: Alternative[F]
}

private[cats] trait ComposedFoldable[F[_], G[_]] extends Foldable[λ[α => F[G[α]]]] { outer =>
  def F: Foldable[F]
  def G: Foldable[G]

  override def foldLeft[A, B](fga: F[G[A]], b: B)(f: (B, A) => B): B =
    F.foldLeft[G[A], B](fga, b)((b, ga) => G.foldLeft[A, B](ga, b)(f))

  override def foldRight[A, B](fga: F[G[A]], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
    F.foldRight[G[A], B](fga, lb)((ga, lb) => G.foldRight[A, B](ga, lb)(f))
}

private[cats] trait ComposedTraverse[F[_], G[_]]
  extends Traverse[λ[α => F[G[α]]]]
    with ComposedFoldable[F, G]
    with ComposedFunctor[F, G] {
  def F: Traverse[F]
  def G: Traverse[G]

  override def traverse[H[_]: Applicative, A, B](fga: F[G[A]])(f: A => H[B]): H[F[G[B]]] =
    F.traverse[H, G[A], G[B]](fga)(ga => G.traverse[H, A, B](ga)(f))
}

private[cats] trait ComposedNonEmptyTraverse[F[_], G[_]]
  extends NonEmptyTraverse[λ[α => F[G[α]]]]
    with ComposedTraverse[F, G]
    with ComposedReducible[F, G] {
  def F: NonEmptyTraverse[F]
  def G: NonEmptyTraverse[G]

  override def nonEmptyTraverse[H[_]: Apply, A, B](fga: F[G[A]])(f: A => H[B]): H[F[G[B]]] =
    F.nonEmptyTraverse[H, G[A], G[B]](fga)(ga => G.nonEmptyTraverse[H, A, B](ga)(f))
}

private[cats] trait ComposedReducible[F[_], G[_]] extends Reducible[λ[α => F[G[α]]]] with ComposedFoldable[F, G] { outer =>
  def F: Reducible[F]
  def G: Reducible[G]

  override def reduceLeftTo[A, B](fga: F[G[A]])(f: A => B)(g: (B, A) => B): B = {
    def toB(ga: G[A]): B = G.reduceLeftTo[A, B](ga)(f)(g)
    F.reduceLeftTo[G[A], B](fga)(toB) { (b, ga) =>
      G.foldLeft[A, B](ga, b)(g)
    }
  }

  override def reduceRightTo[A, B](fga: F[G[A]])(f: A => B)(g: (A, Eval[B]) => Eval[B]): Eval[B] = {
    def toB(ga: G[A]): B = G.reduceRightTo[A, B](ga)(f)(g).value
    F.reduceRightTo[G[A], B](fga)(toB) { (ga, lb) =>
      G.foldRight[A, B](ga, lb)(g)
    }
  }
}

private[cats] trait ComposedContravariant[F[_], G[_]] extends Functor[λ[α => F[G[α]]]] { outer =>
  def F: Contravariant[F]
  def G: Contravariant[G]

  override def map[A, B](fga: F[G[A]])(f: A => B): F[G[B]] =
    F.contramap[G[A], G[B]](fga)(gb => G.contramap[B, A](gb)(f))
}

private[cats] trait ComposedContravariantCovariant[F[_], G[_]] extends Contravariant[λ[α => F[G[α]]]] { outer =>
  def F: Contravariant[F]
  def G: Functor[G]

  override def contramap[A, B](fga: F[G[A]])(f: B => A): F[G[B]] =
    F.contramap[G[A], G[B]](fga)(gb => G.map[B, A](gb)(f))
}

private[cats] trait ComposedApplicativeContravariantMonoidal[F[_], G[_]]
  extends ContravariantMonoidal[λ[α => F[G[α]]]] { outer =>
  def F: Applicative[F]
  def G: ContravariantMonoidal[G]

  override def unit: F[G[Unit]] = F.pure[G[Unit]](G.unit)

  override def contramap[A, B](fa: F[G[A]])(f: B => A): F[G[B]] =
    F.map[G[A], G[B]](fa)(G.contramap[A, B](_)(f))

  override def product[A, B](fa: F[G[A]], fb: F[G[B]]): F[G[(A, B)]] =
    F.map2[G[A], G[B], G[(A, B)]](fa, fb)(G.product[A, B](_, _))
}

private[cats] trait ComposedSemigroupal[F[_], G[_]]
  extends ContravariantSemigroupal[λ[α => F[G[α]]]]
    with ComposedContravariantCovariant[F, G] { outer =>
  def F: ContravariantSemigroupal[F]
  def G: Functor[G]

  def product[A, B](fa: F[G[A]], fb: F[G[B]]): F[G[(A, B)]] =
    F.contramap[(G[A], G[B]), G[(A, B)]](F.product[G[A], G[B]](fa, fb)) { (g: G[(A, B)]) =>
      (G.map[(A, B), A](g)(_._1), G.map[(A, B), B](g)(_._2))
    }
}

private[cats] trait ComposedInvariantApplySemigroupal[F[_], G[_]]
  extends InvariantSemigroupal[λ[α => F[G[α]]]]
    with ComposedInvariantCovariant[F, G] { outer =>
  def F: InvariantSemigroupal[F]
  def G: Apply[G]

  def product[A, B](fa: F[G[A]], fb: F[G[B]]): F[G[(A, B)]] =
    F.imap[(G[A], G[B]), G[(A, B)]](F.product[G[A], G[B]](fa, fb)) {
      case (ga, gb) =>
        G.map2[A, B, (A, B)](ga, gb)(scala.Predef.ArrowAssoc(_) -> _)
    } { (g: G[(A, B)]) =>
      (G.map[(A, B), A](g)(_._1), G.map[(A, B), B](g)(_._2))
    }
}

private[cats] trait ComposedCovariantContravariant[F[_], G[_]] extends Contravariant[λ[α => F[G[α]]]] { outer =>
  def F: Functor[F]
  def G: Contravariant[G]

  override def contramap[A, B](fga: F[G[A]])(f: B => A): F[G[B]] =
    F.map[G[A], G[B]](fga)(ga => G.contramap[A, B](ga)(f))
}

private[cats] trait ComposedInvariantCovariant[F[_], G[_]] extends Invariant[λ[α => F[G[α]]]] { outer =>
  def F: Invariant[F]
  def G: Functor[G]

  override def imap[A, B](fga: F[G[A]])(f: A => B)(g: B => A): F[G[B]] =
    F.imap[G[A], G[B]](fga)(ga => G.map[A, B](ga)(f))(gb => G.map[B, A](gb)(g))
}

private[cats] trait ComposedInvariantContravariant[F[_], G[_]] extends Invariant[λ[α => F[G[α]]]] { outer =>
  def F: Invariant[F]
  def G: Contravariant[G]

  override def imap[A, B](fga: F[G[A]])(f: A => B)(g: B => A): F[G[B]] =
    F.imap[G[A], G[B]](fga)(ga => G.contramap[A, B](ga)(g))(gb => G.contramap[B, A](gb)(f))
}