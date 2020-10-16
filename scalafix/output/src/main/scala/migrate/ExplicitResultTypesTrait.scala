package migrate

object ExplicitResultTypesTrait {
  trait Trait {
    def foo: Map[Int, String]
    def message: CharSequence
  }

  object Overrides extends Trait {
    val foo: scala.collection.immutable.Map[Int,Nothing] = Map.empty[scala.Int, scala.Nothing]
    val message: String = s"hello $foo"
  }
}
