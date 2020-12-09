package prepareMigration

object AutoApplication {
  trait Chunk {
    def bytes(): Seq[Byte]
    def toSeq: Seq[Byte] = bytes()
  }
}
