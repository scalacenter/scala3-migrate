/*
rule = MigrationRule
*/
package incompat

import scala.reflect.ClassTag

object ScalafixIncompat5 {
  def newArray[A: ClassTag]: Array[A] = {
    val size = 5
    val result = new Array(size)
    result
  }
}
