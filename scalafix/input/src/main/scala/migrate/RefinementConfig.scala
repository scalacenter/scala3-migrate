/*
rules = ExplicitResultTypes
ExplicitResultTypes.rewriteStructuralTypesToNamedSubclass = false
*/
package migrate

object RefinementConfig {
  val subclass = new Seq[Int] {
    val accidentalPublic = 42
    def apply(idx: Int): Int = ???
    def iterator: Iterator[Int] = ???
    def length: Int = ???
  }
}
