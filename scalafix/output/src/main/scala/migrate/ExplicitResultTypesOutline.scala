package migrate

object ExplicitResultTypesOutline {
  def foo: Serializable{def format: Int} = new java.io.Serializable {
    def format: Int = 24
  }
}
