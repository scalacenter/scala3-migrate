package migrate


import java.nio.file.Files

import coursier._
import domain.{AbsolutePath, Classpath}
import interfaces.DottyCompiler
import scalafix.interfaces.Scalafix
import utils.CoursierApi

import scala.jdk.CollectionConverters._
import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.reporters.ConsoleReporter
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object Main {

  def compileInScala213(file: AbsolutePath): Try[Unit] = {
    val settings = new Settings()
    val vd = new VirtualDirectory("(memory)", None)
    settings.outputDirs.setSingleOutput(vd)
    settings.classpath.value = Migrate.classpathScala213.value
    val reporter = new ConsoleReporter(settings)
    val global = new Global(settings, reporter)
    Try(new global.Run().compile(List(file.value)))
      .recover { case NonFatal(e) => scribe.info(s"compile with dotty failed with ${e.getMessage}") }
  }

  def compileInDotty(file: AbsolutePath): Try[Unit] = {
    val tempdir = Files.createTempDirectory("dotty-output")
    val settings = Array(
      "-classpath",
      Migrate.classpathDotty.value,
      "-d",
      tempdir.toString,
      file.value)
    Try(DottyCompiler.compile(settings))
      .recover { case NonFatal(e) => scribe.info(s"compile with dotty failed with ${e.getMessage}") }
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

  val classpathScala213: Classpath = CoursierApi.getClasspath(scala213Dep)
  val classpathDotty: Classpath = CoursierApi.getClasspath(dottyDep)

}