/*
rule = [InferTypes, ExplicitImplicits]
*/
package migrate

import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions
import scala.util.Success

object ExplicitResultTypesShort {
  implicit val x = List.empty[Map[Int, Set[String]]]
  implicit val y = HashMap.empty[String, Success[ListBuffer[Int]]]
  implicit def z(x: Int) = List.empty[String]
  implicit var zz = scala.collection.immutable.ListSet.empty[String]
  implicit val FALSE = (x: Any) => false
  implicit def tparam[T](e: T) = e
  implicit val opt = Option.empty[Int]
  implicit val seq = Seq.empty[List[Int]]
  object Shadow {
    val Option = scala.collection.mutable.ListBuffer
    implicit val shadow = Option.empty[List[Int]]
  }
}
