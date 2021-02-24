/*
rule = [MigrationRule]
*/
package implicits

object EtaExpansion {
  def provide(s: String)(implicit v: Int): String = s

  def function(values: Seq[String])(implicit v: Int, s: String): Seq[String] = {
    values.map(x => EtaExpansion.this.provide(x))
    values.map(provide _)
    values.map(EtaExpansion.this.provide _)
    values.map(EtaExpansion.this.provide)
    values.map(EtaExpansion.this.provide(_))
  }
  def foo(a: Int, b: Int)(implicit v: Int, s: String): Int = ???

  def function2(values: Seq[Int])(implicit v: Int, s: String): Int = {
    values.fold(0)(foo)
  }

  implicit val value: Char = '+'
  def foo2(a: Int)(b: Int)(implicit value: Char): Int = a + b
  def bar(f: Int => Int => Int): Int = ???
  bar(EtaExpansion.this.foo2)
  bar(foo2)
  bar(foo2 _)
  bar(a => b => foo2(a)(b))
  bar(a => b => EtaExpansion.this.foo2(a)(b))
  
}
