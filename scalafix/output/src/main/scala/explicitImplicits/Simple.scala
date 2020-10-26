package explicitImplicits

object Simple {
  case class Person(name: String) {
    import Person._
    implicit val crazy1 = implicitly[Int](explicitImplicits.Simple.Person.age)
  }

  object Person {
    implicit val age: Int = 24
  }
}
