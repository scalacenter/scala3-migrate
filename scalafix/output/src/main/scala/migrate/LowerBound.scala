package migrate

object LowerBound {
  trait Bound
  class City extends Bound
  class Town extends Bound
  class Village extends Bound
  sealed abstract class BoundObject[T <: Bound] {
    def list(): List[Bound] = Nil
  }
  case object A extends BoundObject[City]
  case object B extends BoundObject[Town]
  case object C extends BoundObject[Village]
  val x: scala.collection.immutable.List[Product with migrate.LowerBound.BoundObject[_ >: migrate.LowerBound.Village with migrate.LowerBound.Town with migrate.LowerBound.City <: migrate.LowerBound.Bound] with java.io.Serializable] = List(A, B, C)
}