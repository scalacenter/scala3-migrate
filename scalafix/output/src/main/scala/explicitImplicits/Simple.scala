package explicitImplicits

import scala.concurrent.duration._

object Simple {
  case class Person(name: String) {
    import Person._
    implicit val crazy1 = implicitly[Int](explicitImplicits.Simple.Person.age)
      
    val minute = DurationInt(1).minute
  }

  object Person {
    implicit val age: Int = 24
  }
}