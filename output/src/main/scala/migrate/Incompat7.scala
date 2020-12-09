import scala.collection.Factory

object Incompat7 {
  trait Reader[A]
  trait Writer[A]

  object Test {
    def rw[T: Reader: Writer](a: T): Unit = ???

    implicit def seqLikeReader[C[_], T](implicit r: Reader[T], factory: Factory[T, C[T]]): Reader[C[T]] = ???
    implicit def mapReader[A: Reader, B: Reader]: Reader[Map[A, B]] = ???
    implicit val intReader: Reader[Int] = ???

    implicit val writer: Writer[Map[List[Int], Int]] = ???

    rw(Map.apply[List[Int], Int](List(1) -> 1))
  }
}