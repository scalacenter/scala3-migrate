/*
rules = Infertypes
*/
package fix.explicitResultTypes

object ScalaPackage {
  class Try
  def a = null.asInstanceOf[scala.util.Try[Int]]
}
