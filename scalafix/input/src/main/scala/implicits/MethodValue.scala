/*
rule = [MigrationRule]
*/
package implicits

object MethodValue {
  def provide(s: String)(implicit v: Int): String = s
  
  def function(values: Seq[String])(implicit v: Int, s: String): Seq[String] = {
    values.map(x => MethodValue.this.provide(x))
    values.map(provide)
    values.map(MethodValue.this.provide)
    values.map(MethodValue.this.provide(_))
  }
}
