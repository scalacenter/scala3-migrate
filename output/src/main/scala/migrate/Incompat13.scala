object Incompat13 {
  object Test {
    val str =  scala.Predef.any2stringadd(new AnyRef) + "foo"
  }
}