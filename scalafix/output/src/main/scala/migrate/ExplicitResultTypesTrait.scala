package migrate

object ExplicitResultTypesTrait {
  trait Trait {
    def foo: Map[Int, String]
    def message: CharSequence
  }

  object Overrides extends Trait {
    val foo: scala.collection.immutable.Map[Int,Nothing] = Map.empty[Int, Nothing]
    var foo2: scala.collection.immutable.Map[Nothing,Nothing] = Map.empty[Nothing, Nothing]
    def foo3: scala.collection.immutable.Map[Nothing,Nothing] = Map.empty[Nothing, Nothing]
    val message: String = s"hello $foo"
  }
}
