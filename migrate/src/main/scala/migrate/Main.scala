package migrate


import coursier._
import domain.AbsolutePath
import interfaces.DottyCompiler
import scalafix.interfaces.Scalafix
import utils.CoursierApi

import scala.jdk.CollectionConverters._
import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.reporters.ConsoleReporter
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object Main {

  def compileInScala213(file: AbsolutePath): Try[Unit] = {
    val settings = new Settings()
    settings.classpath.value =  Migrate.classpathScala213.value
    val reporter = new ConsoleReporter(settings)
    val global = new Global(settings, reporter)
    Try(new global.Run().compile(List(file.value)))
      .recover{ case NonFatal(e) => scribe.info(s"compile with dotty failed with ${e.getMessage}")}
  }

  def compileInDotty(file: AbsolutePath): Try[Unit] = {
    Try(DottyCompiler.compile(Array("-classpath", Migrate.classpathDotty.value, file.value)))
      .recover{ case NonFatal(e) => scribe.info(s"compile with dotty failed with ${e.getMessage}")}
  }

  def runScalafix(file: AbsolutePath): Try[String] = {
    val api = Scalafix.fetchAndClassloadInstance("2.13")
    val eval = api.newArguments()
      .withRules(List("ProcedureSyntax").asJava)
      .withPaths(List(file.toPath).asJava)
      .evaluate()

    eval.getFileEvaluations.headOption match {
      case Some(fileEval) => Success(fileEval.previewPatches().get)
      case None => Failure(new Exception(s"Scalafix failed with error ${eval.getErrors.toList}"))
    }

  }


}
object Migrate {
  private val scala213Dep: Dependency =
    dep"org.scala-lang:scala-library:2.13.3"

  private val dottyDep: Dependency =
    dep"ch.epfl.lamp:dotty-library_0.27:0.27.0-RC1"

  val classpathScala213 =  CoursierApi.getClasspath(scala213Dep)
  val classpathDotty = CoursierApi.getClasspath(dottyDep)

}