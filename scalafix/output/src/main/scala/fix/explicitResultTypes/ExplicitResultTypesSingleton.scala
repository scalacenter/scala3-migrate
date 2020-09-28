package fix.explicitResultTypes

object ExplicitResultTypesSingleton {
  implicit val default: fix.explicitResultTypes.ExplicitResultTypesSingleton.type = ExplicitResultTypesSingleton
  implicit val singleton: fix.explicitResultTypes.ExplicitResultTypesSingleton2.Singleton.type = ExplicitResultTypesSingleton2.Singleton
}
object ExplicitResultTypesSingleton2 {
  object Singleton
}
