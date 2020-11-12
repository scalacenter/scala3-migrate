package migrate

package object testpkg {

  object O1 {
    class C1

    def foo: O2.C2 = new O2.C2
  }

  object O2 {
    class C2
  }

  class O3

  type O4 = PartialFunction[String, Int]
}