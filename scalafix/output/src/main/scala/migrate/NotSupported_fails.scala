package migrate

object NotSupported_fails {

  object TypesHelpers {
    class C
    class E {
      class C
    }
    class P {
      class C
      val c: C = ???
    }
    val p = new P
    val c = p.c
    trait A
    trait B
    class ann extends scala.annotation.StaticAnnotation
    class H[M[_]]
  }

  class H[M[_]]

  val universalType1 = ??? : H[({ type L[U] = List[U] })#L]

  // FIXME: https://github.com/twitter/rsc/issues/144
//   val repeatedType = ??? : ((Any*) => Any)

}
