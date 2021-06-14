package migrateSyntax

object Any2String {
  val str: String = String.valueOf(new AnyRef) + "foo"
}
