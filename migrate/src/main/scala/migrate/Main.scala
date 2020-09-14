package migrate


import interfaces.DottyCompiler
import scalafix.interfaces.Scalafix
import utils.{AbsolutePath, Classpath}

import scala.jdk.CollectionConverters._
import scala.tools.nsc.Settings
import scala.tools.nsc.Global
import scala.tools.nsc.reporters.ConsoleReporter
import scala.util.{Failure, Success, Try}

object Main {
  def compileInScala213(file: AbsolutePath): Unit = {
    val settings = new Settings()
    settings.classpath.value =  Migrate.classpathScala213.value
    val reporter = new ConsoleReporter(settings)
    val global = new Global(settings, reporter)
    Try(new global.Run().compile(List(file.value))) match {
      case Success(_) => println("compileInScala213 success")
      case Failure(e) => println(s"compileInScala213 failure: ${e.getMessage}")
    }
  }
  def compileInDotty(file: AbsolutePath): Unit = {
    Try(DottyCompiler.compile(Array("-classpath", Migrate.classpathDotty.value, file.value))) match {
      case Success(_) => println("compileInDotty success")
      case Failure(e) => println(s"compileInDotty failure: ${e.getMessage}")
    }
  }

  def runScalafix(file: AbsolutePath): Unit = {
    val api = Scalafix.fetchAndClassloadInstance("2.13")
    val eval = api.newArguments()
      .withRules(List("ProcedureSyntax").asJava)
      .withPaths(List(file.toPath).asJava)
      .evaluate()

    eval.getFileEvaluations.headOption match {
      case Some(fileEval) => println(s"Scalafix success ${fileEval.previewPatches().get}")
      case None => println(s"Scalafix failed with error ${eval.getErrors.toList}")
    }

  }


}
object Migrate {
  private val scala213Lib: AbsolutePath = AbsolutePath("/Users/meriamlachkar/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.3/scala-library-2.13.3.jar")
  private val dottyLib: AbsolutePath = AbsolutePath("/Users/meriamlachkar/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/ch/epfl/lamp/dotty-library_0.27/0.27.0-RC1/dotty-library_0.27-0.27.0-RC1.jar")

  val classpathDotty = Classpath(scala213Lib, dottyLib)
  val classpathScala213 = Classpath(scala213Lib)
}