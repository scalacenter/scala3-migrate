package utils

import coursier._
import domain.{AbsolutePath, Classpath}

object CoursierApi {

  def getClasspath(dep: Dependency): Classpath = {
    val paths = Fetch()
      .addDependencies(dep)
      .run()
    Classpath(paths.map(AbsolutePath.from): _*)
  }

}
