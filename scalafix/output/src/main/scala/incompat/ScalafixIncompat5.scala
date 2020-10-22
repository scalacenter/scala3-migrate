package incompat

import scala.reflect.ClassTag

object ScalafixIncompat5 {
  def newArray[A: ClassTag]: Array[A] = {
    val size: Int = 5
    val result: Array[A] = new Array(size)
    result
  }
}
