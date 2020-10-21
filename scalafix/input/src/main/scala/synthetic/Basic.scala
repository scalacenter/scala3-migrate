/*
rule = MigrationRule
*/
package synthetic

object Basic {
  val people = List(1).map(_ + 1).map(elm => Map(elm -> elm))

  for {
    a <- List(1)
    b = a + 1 // assignment
  } yield a + b

  for {
    (a, b) <- List(1 -> 2) // pattern
  } yield a + b

  val listOfTEst = List(Other.Test(1))
  def a[A](in: A) = List(in)
}

object Other {
  case class Test(i: Int)
}

class Test {
  case class Ok(s: String)
  val list = List(Ok("ok"))
}