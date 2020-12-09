
package types

// Don't infer
object StructuralRefinement {
  val param = new { lazy val default: Boolean = true }
  val more1: AnyRef{def x: Int; def x_=(x$1: Int): Unit} = new { var x: Int = 42 }
  val more2 = new { def foo(implicit x: Int, y: Int): Int = 42 }
  val more3 = new { implicit def bar: Int = 42 }
}