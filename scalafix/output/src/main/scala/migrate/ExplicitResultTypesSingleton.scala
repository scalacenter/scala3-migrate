package migrate

object ExplicitResultTypesSingleton {
  implicit val default: migrate.ExplicitResultTypesSingleton.type = ExplicitResultTypesSingleton
  implicit val singleton: migrate.ExplicitResultTypesSingleton2.Singleton.type = ExplicitResultTypesSingleton2.Singleton
}
object ExplicitResultTypesSingleton2 {
  object Singleton
}