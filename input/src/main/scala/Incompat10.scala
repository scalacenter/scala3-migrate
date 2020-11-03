object Incompat10 {

  trait Tupler[A, B] {
    type Out

    def apply(a: A, b: B): Out
  }

  trait Foo[A] {
    def product[B](that: Foo[B])(implicit tupler: Tupler[A, B]): Foo[tupler.Out]
  }

  trait Bar[A] {
    def run(): Option[Foo[A]]
  }

  trait Quux {

    def product[A, B](first: Bar[A], second: Bar[B])(implicit tupler: Tupler[A, B]): Bar[tupler.Out] =
      () => {
        first.run().flatMap { fooA =>
          second.run().map { fooB =>
            fooA.product(fooB)
          }
        }
      }
  }

}
