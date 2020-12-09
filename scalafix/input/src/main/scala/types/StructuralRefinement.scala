/*
rules = [InferTypes, ExplicitImplicits]
*/

package types

// Don't infer
object StructuralRefinement {
  val param = new { lazy val default = true }
  val more1 = new { var x = 42 }
  val more2 = new { def foo(implicit x: Int, y: Int) = 42 }
  val more3 = new { implicit def bar = 42 }
}