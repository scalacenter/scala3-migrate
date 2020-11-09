package migrate

object RefinementConfig {
  val subclass: Seq[Int]{val accidentalPublic: Int} = new Seq[Int] {
    val accidentalPublic: Int = 42
    def apply(idx: Int): Int = ???
    def iterator: Iterator[Int] = ???
    def length: Int = ???
  }
}