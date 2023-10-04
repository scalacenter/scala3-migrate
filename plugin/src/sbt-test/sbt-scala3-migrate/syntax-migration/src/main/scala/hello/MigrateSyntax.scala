package hello

object MigrateSyntaxTest {
  // ProcedureSyntax
  // fix.scala213.ConstructorProcedureSyntax
  trait Foo {
    def print()
  }

  object Bar {
    def print() {
      println("bar")
    }
  }

  // migrate.ParensAroundParam
  val f1 = { x: Int => x * x }
  val f2 = { (x: Int) => x * x }
  val f3 = (x: Int) => x * x

  // fix.scala213.ExplicitNonNullaryApply
  trait Chunk {
    def bytes(): Seq[Byte]

    def toSeq: Seq[Byte] = bytes
  }

  // fix.scala213.ExplicitNullaryEtaExpansion
  val x            = 1
  val g: () => Int = x _

  // fix.scala213.Any2StringAdd
  val str = new AnyRef + "foo"
}
