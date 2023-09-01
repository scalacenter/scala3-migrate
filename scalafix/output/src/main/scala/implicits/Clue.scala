package implicits

class Clue

object Clue {
  implicit def generate(any: Any): Clue = new Clue
}

object TestClue {
  def test(clue: Clue): Unit = ()

  test(implicits.Clue.generate("foo"))
}


