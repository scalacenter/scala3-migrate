package migrate

object Simple {
  val valeur: String = "juste du text"
  val k: Int = 1
  val int: Int = 1
  val listOfString: Seq[String] = Seq.apply[String]("string")
  val duration: scala.concurrent.duration.FiniteDuration = scala.concurrent.duration.Duration.Zero

  case class User(firstName: String, lastName: String)
  val ml: migrate.Simple.User = User("m", "l")
}