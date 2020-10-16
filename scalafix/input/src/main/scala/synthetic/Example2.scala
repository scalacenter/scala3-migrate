/*
rule = MigrationRule
*/
package synthetic

class Example2 {

  trait Foo {
    type Inner
  }

  object Foo {
    val foo: Foo {type Inner = String} = ???

    def inner(foo: Foo): foo.Inner = ???

    def bar(f: String => Int): List[Int] = Some(inner(foo))
      .flatMap(_ => Some("test")).map(_ => List(0).map(_ + 10)).getOrElse(Nil)
  }

}
