import reflect.ClassTag

object Test1 {
  def newArray[A: ClassTag]: Array[A] = {
    val size = 5
    val result: Array[A] = new Array(size)
    result
  }

  implicit val number: Int = 5
  implicit val crazy1: Int = implicitly[Int](number)
}
