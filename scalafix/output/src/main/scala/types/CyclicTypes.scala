package types

import java.nio.file.Paths

class CyclicTypes {
  class Path
  def path: java.nio.file.Path = Paths.get("")
  object inner {
    val file: java.nio.file.Path = path
    object inner {
      val nio: java.nio.file.Path = path
      object inner {
        val java: _root_.java.nio.file.Path = path
        val test: List[_root_.java.nio.file.Path] = List.apply[_root_.java.nio.file.Path](java)
      }
    }
  }
}