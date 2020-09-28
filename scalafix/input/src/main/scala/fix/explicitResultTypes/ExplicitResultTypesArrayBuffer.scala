/*
rule = Infertypes
*/
package fix.explicitResultTypes

import scala.collection.mutable.ArrayBuffer

object ExplicitResultTypesArrayBuffer {
  def empty[A] = ArrayBuffer.empty[A]
}

