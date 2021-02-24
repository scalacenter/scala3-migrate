package implicits

object MethodValue {
  def provide(s: String)(implicit v: Int): String = s
  
  def function(values: Seq[String])(implicit v: Int, s: String): Seq[String] = {
    values.map[String](x => MethodValue.this.provide(x)(v))
    values.map[String](provide(_)(v))
    values.map[String](MethodValue.this.provide(_)(v))
    values.map[String](MethodValue.this.provide(_)(v))
  }
}