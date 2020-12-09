package migrate

import scala.concurrent.duration._

object Simple {
  val valeur: String = "juste du text"
  val k: Int = 1
  val int: Int = 1
  val listOfString: Seq[String] = Seq.apply[String]("string")
  val duration: scala.concurrent.duration.FiniteDuration = scala.concurrent.duration.Duration.Zero

  case class User(firstName: String, lastName: String)
  val ml: migrate.Simple.User = User("m", "l")

  case class Person(name: String) {
    import Person._
    implicit val crazy1: Int = implicitly[Int](migrate.Simple.Person.age)

    val minute: scala.concurrent.duration.FiniteDuration = DurationInt(1).minute
  }

  object Person {
    implicit val age: Int = 24
  }
}