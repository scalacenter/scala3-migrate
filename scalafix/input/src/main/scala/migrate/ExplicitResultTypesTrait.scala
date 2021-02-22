/*
rule = [MigrationRule]
*/
package migrate

object ExplicitResultTypesTrait {
  trait Trait {
    def foo: Map[Int, String]
    def message: CharSequence
  }

  object Overrides extends Trait {
    val foo = Map.empty
    var foo2 = Map.empty
    def foo3 = Map.empty
    val message = s"hello $foo"
  }
}
