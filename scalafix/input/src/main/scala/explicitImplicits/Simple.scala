/*
rules = ExplicitImplicits
*/
package explicitImplicits

import scala.concurrent.duration._

object Simple {
  case class Person(name: String) {
    import Person._
    implicit val crazy1 = implicitly[Int]
      
    val minute = 1.minute
  }

  object Person {
    implicit val age: Int = 24
  }
}
