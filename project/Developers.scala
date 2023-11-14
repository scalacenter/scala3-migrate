import sbt._

object Developers {
  val adpi2: Developer =
    Developer("adpi2", "Adrien Piquerez", "adrien.piquerez@gmail.com", url("https://github.com/adpi2"))

  val mlachkar = Developer("mlachkar", "Meriam Lachkar", "meriam.lachkar@gmail.com", url("https://github.com/mlachkar"))

  val list: List[Developer] = List(adpi2, mlachkar)
}
