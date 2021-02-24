package implicits

object EtaExpansion {
  def provide(s: String)(implicit v: Int): String = s

  def function(values: Seq[String])(implicit v: Int, s: String): Seq[String] = {
    values.map[String](x => EtaExpansion.this.provide(x)(v))
    values.map[String](provide(_)(v))
    values.map[String](EtaExpansion.this.provide(_)(v))
    values.map[String](EtaExpansion.this.provide(_)(v))
    values.map[String](EtaExpansion.this.provide(_)(v))
  }
  def foo(a: Int, b: Int)(implicit v: Int, s: String): Int = ???

  def function2(values: Seq[Int])(implicit v: Int, s: String): Int = {
    values.fold[Int](0)(foo(_, _)(v, s))
  }

  implicit val value: Char = '+'
  def foo2(a: Int)(b: Int)(implicit value: Char): Int = a + b
  def bar(f: Int => Int => Int): Int = ???
  bar(EtaExpansion.this.foo2)
  bar(foo2)
  bar(foo2 _)
  bar(a => b => foo2(a)(b)(implicits.EtaExpansion.value))
  bar(a => b => EtaExpansion.this.foo2(a)(b)(implicits.EtaExpansion.value))
  
}