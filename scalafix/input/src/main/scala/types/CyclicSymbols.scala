/*
rules = [MigrationRule]
*/
package types

object CyclicSymbols {

  case class Person(name: String) {
    import Person._
    val types = "test"
    implicit val crazy1 = implicitly[Int]

  }

  object Person {
    implicit val age: Int = 24
  }
}
