package synthetic

object Basic {
  def func(name: String)(age1: Int, age2: Int)(implicit email: String, number: Int = 5): Unit = println(s"number = ${number}")
  implicit val email: String = "a@b.com"
  implicit val age: Int = 5

  val person: Unit = func("jon")(28, 30)(email,age)
  val person2: Unit = func("jon")(28, 30)(email)

  val people: scala.collection.immutable.List[scala.collection.immutable.Map[Int,Int]] = List.apply[Int](1).map[Int](_ + 1).map[Map[Int,Int]](elm => Map.apply[Int,Int](elm -> elm))

  for {
    a <- List.apply[Int](1)
    b = a + 1 // assignment
  } yield a + b

  for {
    (a, b) <- List.apply[Tuple2[Int,Int]](1 -> 2) // pattern
  } yield a + b
}