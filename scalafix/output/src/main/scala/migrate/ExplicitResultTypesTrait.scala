package migrate

object ExplicitResultTypesTrait {
  trait Trait {
    def foo: Map[Int, String]
    def message: CharSequence
  }

  object Overrides extends Trait {
    val foo: Map[Int,Nothing] = Map.empty[Int, Nothing]
    var foo2: Map[Nothing,Nothing] = Map.empty[Nothing, Nothing]
    def foo3: Map[Nothing,Nothing] = Map.empty[Nothing, Nothing]
    val message: String = s"hello $foo"
  }
}