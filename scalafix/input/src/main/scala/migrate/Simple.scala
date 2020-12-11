/*
rule = [InferTypes, ExplicitImplicits]
*/
package migrate

import scala.concurrent.duration._

object Simple {
  val valeur = "juste du text"
  val k = 1
  val int = 1
  val listOfString = Seq("string")
  val duration = scala.concurrent.duration.Duration.Zero

  case class User(firstName: String, lastName: String)
  val ml = User("m", "l")

  case class Person(name: String) {
    import Person._
    implicit val crazy1 = implicitly[Int]

    val minute = 1.minute
  }

  object Person {
    implicit val age: Int = 24
  }
}
