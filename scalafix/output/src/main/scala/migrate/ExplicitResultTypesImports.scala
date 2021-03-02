package migrate

import scala.util._

object ExplicitResultTypesImports {

  val x: collection.immutable.ListSet[Int] = scala.collection.immutable.ListSet.empty[Int]
  // val Either = scala.util.Either

  val duplicate1: concurrent.duration.FiniteDuration = null.asInstanceOf[scala.concurrent.duration.FiniteDuration]
  val duplicate2: concurrent.duration.FiniteDuration = null.asInstanceOf[scala.concurrent.duration.FiniteDuration]

  val timezone: java.util.TimeZone = null.asInstanceOf[java.util.TimeZone]

  val inner: collection.Searching.SearchResult = null.asInstanceOf[scala.collection.Searching.SearchResult]

  final val javaEnum = java.util.Locale.Category.DISPLAY

  type MyResult = Either[Int, String]
  val inferTypeAlias: Either[Int,String] = null.asInstanceOf[Either[Int, String]]

  val wildcardImport: Try[Int] = Try.apply[Int](1)

  sealed abstract class ADT
  object ADT {
    case object A extends ADT
    case object B extends ADT
  }
  val productWithSerializable: List[Product with ADT with Serializable] = List.apply[Product with ADT with Serializable](ADT.A, ADT.B)

  sealed abstract class ADT2
  trait Mixin[T]
  object ADT2 {
    case object A extends ADT2 with Mixin[Int]
    case object B extends ADT2 with Mixin[String]
    case object C extends ADT2 with Mixin[Int]
  }
  val longSharedParent1: List[Product with ADT2 with Mixin[_ >: String with Int] with Serializable] = List.apply[Product with ADT2 with Mixin[_ >: String with Int] with Serializable](ADT2.A, ADT2.B)
  val longSharedParent2: List[Product with ADT2 with Mixin[Int] with Serializable] = List.apply[Product with ADT2 with Mixin[Int] with Serializable](ADT2.A, ADT2.C)

  val juMap: java.util.Map[Int,String] = java.util.Collections.emptyMap[Int, String]()
}