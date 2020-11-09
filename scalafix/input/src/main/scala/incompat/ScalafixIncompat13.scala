/*
rule = [MigrationRule, ExplicitImplicits]
*/
package incompat

object ScalafixIncompat13 {

  object Test {
    val str = new AnyRef + "foo"
  }

}
