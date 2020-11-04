package migrate

import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions
import scala.util.Success

object ExplicitResultTypesShort {
  implicit val x: List[Map[Int,Set[String]]] = List.empty[Map[Int, Set[String]]]
  implicit val y: scala.collection.immutable.HashMap[String,scala.util.Success[scala.collection.mutable.ListBuffer[Int]]] = HashMap.empty[String, Success[ListBuffer[Int]]]
  implicit def z(x: Int): List[String] = List.empty[String]
  implicit var zz: scala.collection.immutable.ListSet[String] = scala.collection.immutable.ListSet.empty[String]
  implicit val FALSE: Any => Boolean = (x: Any) => false
  implicit def tparam[T](e: T): T = e
  implicit val opt: Option[Int] = Option.empty[Int]
  implicit val seq: Seq[List[Int]] = Seq.empty[List[Int]]
  object Shadow {
    val Option: collection.mutable.ListBuffer.type = scala.collection.mutable.ListBuffer
    implicit val shadow: scala.collection.mutable.ListBuffer[List[Int]] = Option.empty[List[Int]]
  }
}
