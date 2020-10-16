
package migrate

import scala.util._

object ExplicitResultTypesImports {

  val x: scala.collection.immutable.ListSet[Int] = scala.collection.immutable.ListSet.empty[Int]
  // val Either = scala.util.Either

  val duplicate1: scala.concurrent.duration.FiniteDuration = null.asInstanceOf[scala.concurrent.duration.FiniteDuration]
  val duplicate2: scala.concurrent.duration.FiniteDuration = null.asInstanceOf[scala.concurrent.duration.FiniteDuration]

  val timezone: java.util.TimeZone = null.asInstanceOf[java.util.TimeZone]

  val inner: collection.Searching.SearchResult = null.asInstanceOf[scala.collection.Searching.SearchResult]

  final val javaEnum = java.util.Locale.Category.DISPLAY

  type MyResult = Either[Int, String]
  val inferTypeAlias: scala.util.Either[Int,String] = null.asInstanceOf[Either[Int, String]]

  val wildcardImport: scala.util.Try[Int] = Try.apply[Int](1)

  sealed abstract class ADT
  object ADT {
    case object A extends ADT
    case object B extends ADT
  }
  val productWithSerializable: scala.collection.immutable.List[Product with migrate.ExplicitResultTypesImports.ADT with java.io.Serializable] = List.apply[Product with migrate.ExplicitResultTypesImports.ADT with java.io.Serializable](ADT.A, ADT.B)

  sealed abstract class ADT2
  trait Mixin[T]
  object ADT2 {
    case object A extends ADT2 with Mixin[Int]
    case object B extends ADT2 with Mixin[String]
    case object C extends ADT2 with Mixin[Int]
  }
  val longSharedParent1: scala.collection.immutable.List[Product with migrate.ExplicitResultTypesImports.ADT2 with migrate.ExplicitResultTypesImports.Mixin[_ >: String with Int] with java.io.Serializable] = List.apply[Product with migrate.ExplicitResultTypesImports.ADT2 with migrate.ExplicitResultTypesImports.Mixin[_ >: String with Int] with java.io.Serializable](ADT2.A, ADT2.B)
  val longSharedParent2: scala.collection.immutable.List[Product with migrate.ExplicitResultTypesImports.ADT2 with migrate.ExplicitResultTypesImports.Mixin[Int] with java.io.Serializable] = List.apply[Product with migrate.ExplicitResultTypesImports.ADT2 with migrate.ExplicitResultTypesImports.Mixin[Int] with java.io.Serializable](ADT2.A, ADT2.C)

  val juMap: java.util.Map[Int,String] = java.util.Collections.emptyMap[Int, String]()
}
