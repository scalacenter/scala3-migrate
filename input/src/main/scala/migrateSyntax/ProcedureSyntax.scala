package migrateSyntax

object ProcedureSyntax {
  class Bar(a: String) {
    def this() {
      this("bar")
      print()
    }

    def print() {
      println(a)
    }
  }
}
