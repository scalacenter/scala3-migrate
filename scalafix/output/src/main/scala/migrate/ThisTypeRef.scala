package migrate

object ThisTypeRef {
  trait Base {
    class T
  }
  class Sub extends Base {
    val ref: Sub.this.T = identity[Sub.this.T](new T())
  }

  trait ThisType  {
    def cp(): this.type
  }
  class ThisTypeImpl extends ThisType {
    def cp(): ThisTypeImpl.this.type = this
  }

}
