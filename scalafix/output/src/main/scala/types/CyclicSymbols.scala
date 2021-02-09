package types

object CyclicSymbols {

  case class Person(name: String) {
    import Person._
    val types: String = "test"
    implicit val crazy1: Int = implicitly[Int](_root_.types.CyclicSymbols.Person.age)

  }

  object Person {
    implicit val age: Int = 24
  }
}
