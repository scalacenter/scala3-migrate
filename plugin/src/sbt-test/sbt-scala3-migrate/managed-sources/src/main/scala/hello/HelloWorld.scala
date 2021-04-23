package hello

import buildinfo.BuildInfo

object HelloWorld {
  def toto() = {
    val message = s"Built with ${BuildInfo.scalaVersion}"
    println("message")
  }
}
