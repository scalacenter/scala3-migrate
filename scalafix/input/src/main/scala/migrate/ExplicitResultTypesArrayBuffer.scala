/*
rule = [MigrationRule]
*/
package migrate

import scala.collection.mutable.ArrayBuffer

object ExplicitResultTypesArrayBuffer {
  def empty[A] = ArrayBuffer.empty[A]
}

