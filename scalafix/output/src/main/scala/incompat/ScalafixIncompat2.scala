package incompat

object ScalafixIncompat2 {

  trait Context {
    type Out
  }

  object Context {
    def foo(implicit ctx: Context): Option[ctx.Out] = ???

    def bar(implicit ctx: Context): (Option[ctx.Out], String) = (foo(ctx), "foo")
  }

}