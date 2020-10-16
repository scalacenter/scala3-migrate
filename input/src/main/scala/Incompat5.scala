import reflect.ClassTag

object Incompat5 {
  def newArray[A: ClassTag]: Array[A] = {
    val size = 5
    val result = new Array(size)
    result
  }
}
