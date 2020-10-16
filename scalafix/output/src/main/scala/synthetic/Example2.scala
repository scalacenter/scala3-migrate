package synthetic

class Example2 {

  trait Foo {
    type Inner
  }

  object Foo {
    val foo: Foo {type Inner = String} = ???

    def inner(foo: Foo): foo.Inner = ???

    def bar(f: String => Int): List[Int] = Some.apply[String](inner(foo))
      .flatMap[String](_ => Some.apply[String]("test")).map[scala.collection.immutable.List[Int]](_ => List.apply[Int](0).map[Int](_ + 10)).getOrElse[scala.collection.immutable.List[Int]](Nil)
  }

}
