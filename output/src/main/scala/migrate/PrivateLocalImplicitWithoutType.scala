object PrivateLocalImplicitWithoutType {
  class MyImplicit[T]

  def foo[T]()(implicit ev: MyImplicit[T]): Unit = ()

  private implicit val intImplicit: PrivateLocalImplicitWithoutType.MyImplicit[Int] = new MyImplicit[Int]

  def test(): Unit = {
    foo[Int]()

    implicit val stringImplicit = new MyImplicit[String]

    foo[String]()
  }
}
