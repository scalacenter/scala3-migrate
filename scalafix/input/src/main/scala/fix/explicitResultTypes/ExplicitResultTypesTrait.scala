/*
rules = "Infertypes"
 */
package fix.explicitResultTypes

object ExplicitResultTypesTrait {
  trait Trait {
    def foo: Map[Int, String]
    def message: CharSequence
  }

  object Overrides extends Trait {
    val foo = Map.empty
    val message = s"hello $foo"
  }
}
