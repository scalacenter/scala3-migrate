package hello

object MigrateSyntaxTest {
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

  //fix.scala213.ParensAroundLambda
  val f = { x: Int => x * x }

  //fix.scala213.ExplicitNonNullaryApply
  trait Chunk {
    def bytes(): Seq[Byte]

    def toSeq: Seq[Byte] = bytes
  }

  //fix.scala213.ExplicitNullaryEtaExpansion
  val x            = 1
  val g: () => Int = x _

  //fix.scala213.Any2StringAdd
  val str = new AnyRef + "foo"
}
