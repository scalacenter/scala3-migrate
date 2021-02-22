/*
rules = [MigrationRule]
*/
package types

import java.nio.file.Paths

class CyclicTypes {
  class Path
  def path = Paths.get("")
  object inner {
    val file = path
    object inner {
      val nio = path
      object inner {
        val java = path
        val test = List(java)
      }
    }
  }
}
