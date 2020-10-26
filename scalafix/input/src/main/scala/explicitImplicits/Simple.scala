/*
rules = ExplicitImplicits
*/
package explicitImplicits

object Simple {
  case class Person(name: String) {
    import Person._
    implicit val crazy1 = implicitly[Int]
  }

  object Person {
    implicit val age: Int = 24
  }
}
