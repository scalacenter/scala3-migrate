package incompat

import scala.collection.Factory

object ScalafixIncompat7 {
  trait Reader[A]
  trait Writer[A]

  object Test {
    def rw[T: Reader: Writer](a: T): Unit = ???

    implicit def seqLikeReader[C[_], T](implicit r: Reader[T], factory: Factory[T, C[T]]): Reader[C[T]] = ???
    implicit def mapReader[A: Reader, B: Reader]: Reader[Map[A, B]] = ???
    implicit val intReader: Reader[Int] = ???

    implicit val writer: Writer[Map[List[Int], Int]] = ???
    
    rw[Map[List[Int],Int]](Map.apply[List[Int], Int](scala.Predef.ArrowAssoc(List.apply[Int](1)) -> 1))(mapReader[List[Int], Int](incompat.ScalafixIncompat7.Test.seqLikeReader, incompat.ScalafixIncompat7.Test.intReader), writer)
  }
}