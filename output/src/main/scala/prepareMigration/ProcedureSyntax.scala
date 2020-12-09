package prepareMigration

object ProcedureSyntax {
  class Bar(a: String) {
    def this() = {
      this("bar")
      print()
    }

    def print(): Unit = {
      println(a)
    }
  }
}
