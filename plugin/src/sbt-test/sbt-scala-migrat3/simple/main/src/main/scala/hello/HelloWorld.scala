package hello

import simple.Simple

object HelloWorld {
  def toto() = {
    val message = s"Welcome to ${Simple.name} using scala version ${Simple.scalaVersion}"
    println("message")
  }
}
