/*
rule = Infertypes
*/
package synthetic

object Basic {
  def func(name: String)(age1: Int, age2: Int)(implicit email: String, number: Int = 5): Unit = println(s"number = ${number}")
  implicit val email: String = "a@b.com"
  implicit val age = 5

  val person = func("jon")(28, 30)
  val person2 = func("jon")(28, 30)(email)

  val people = List(1).map(_ + 1).map(elm => Map(elm -> elm))

  for {
    a <- List(1)
    b = a + 1 // assignment
  } yield a + b

  for {
    (a, b) <- List(1 -> 2) // pattern
  } yield a + b
}
