package implicits

object Mix { 
  import OptionalConversion._
  implicit val int: Int = 15
  implicit val listInt: List[Int] = List.apply[Int](0)
  implicit val name: String = "Name"
  
  def firstFunction()(implicit listAge: List[Int], name: String): String =  s"$name"
  
  val test: Int = FromStrinToInt(firstFunction()(implicits.Mix.listInt, implicits.Mix.name))
}


object OptionalConversion {
  implicit def FromStrinToInt(in: String)(implicit int: Int): Int = int
}

