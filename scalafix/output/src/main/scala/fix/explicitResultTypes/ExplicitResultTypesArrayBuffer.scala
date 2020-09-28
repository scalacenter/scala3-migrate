package fix.explicitResultTypes

import scala.collection.mutable.ArrayBuffer

object ExplicitResultTypesArrayBuffer {
  def empty[A]: scala.collection.mutable.ArrayBuffer[A] = ArrayBuffer.empty[A]
}

