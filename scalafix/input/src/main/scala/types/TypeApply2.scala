/*
rules = [MigrationRule]
*/
package zio

import zio.Chunk

class TypeApply2 {
  implicit class ChunkExtension[+A](self: Chunk[A]) {

    override final def equals(that: Any): Boolean =
      that match {
        case that: Chunk[_] =>
          if (self.length != that.length) false
          else {
            val leftIterator: Iterator[Array[A]]    = self.arrayIterator[A]
            val rightIterator   = that.arrayIterator
            var left: Array[A]  = null
            var right: Array[_] = null
            var leftLength      = 0
            var rightLength     = 0
            var i               = 0
            var j               = 0
            var equal           = true
            var done            = false
            while (equal && !done) {
              if (i < leftLength && j < rightLength) {
                val a1 = left(i)
                val a2 = right(j)
                if (a1 != a2) {
                  equal = false
                }
                i += 1
                j += 1
              } else if (i == leftLength && leftIterator.hasNext) {
                left = leftIterator.next()
                leftLength = left.length
                i = 0
              } else if (j == rightLength && rightIterator.hasNext) {
                right = rightIterator.next()
                rightLength = right.length
                j = 0
              } else if (i == leftLength && j == rightLength) {
                done = true
              } else {
                equal = false
              }
            }
            equal
          }
        case that: Seq[_] =>
          self.corresponds(that)(_ == _)
        case _ => false
      }
  }
}
