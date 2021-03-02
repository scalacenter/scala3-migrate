package migrate

object ScalaPackage {
  class Try
  def a: util.Try[Int] = null.asInstanceOf[scala.util.Try[Int]]
}