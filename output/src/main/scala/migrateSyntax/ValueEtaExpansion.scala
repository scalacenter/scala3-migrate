package migrateSyntax

object ValueEtaExpansion {
  val x = 1
  val f: () => Int = () => x
}
