package hello

object MigrateSyntax {
  //ProcedureSyntax
  //fix.scala213.ConstructorProcedureSyntax
  trait Foo {
    def print()
  }

  object Bar {
    def print() {
      println("bar")
    }
  }

  //fix.scala213.Any2StringAdd
  val str = new AnyRef + "foo"
}
