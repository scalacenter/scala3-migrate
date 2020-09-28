/*
rule = Infertypes
*/
package fix.explicitResultTypes

object Infertypes {
  val valeur = "juste du text"
  val k = 1
  val int = 1
  val listOfString = Seq("string")
  val duration = scala.concurrent.duration.Duration.Zero

  case class User(firstName: String, lastName: String)
  val ml = User("Meriam", "Lachkar")
}
