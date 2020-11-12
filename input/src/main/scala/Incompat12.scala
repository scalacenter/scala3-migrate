object Incompat12 {

  trait Pretty {
    val print: String
  }

  object Pretty {
    def pretty[A](a: A)(implicit ev: A => Pretty): String = a.print
  }

  object Test extends App {
    assert(Pretty.pretty("foo")(str => new Pretty {
      val print = str
    }) == "foo")
  }

}