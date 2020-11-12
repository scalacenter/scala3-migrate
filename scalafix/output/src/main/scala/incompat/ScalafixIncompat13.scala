package incompat

object ScalafixIncompat13 {

  object Test {
    val str: String = scala.Predef.any2stringadd(new AnyRef) + "foo"
  }

}