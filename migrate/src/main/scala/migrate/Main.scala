package migrate


import java.nio.file.Files

import coursier._
import domain.{AbsolutePath, Classpath}
import interfaces.DottyCompiler
import scalafix.interfaces.{Scalafix, ScalafixFileEvaluation}
import utils.CoursierApi
import utils.ScalaExtensions.OptionalExtension

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

  def runScalafix(file: AbsolutePath, sourceRoot: AbsolutePath, classpath: Classpath): Try[ScalafixFileEvaluation] = {
    val api = Scalafix.fetchAndClassloadInstance("2.13")
    val evals = api.newArguments()
      .withScalaVersion("2.13.3")
      .withClasspath(classpath.paths.map(_.toNio).asJava)
      .withToolClasspath(classpath.toUrlClassLoader(api.getClass.getClassLoader))
      .withSourceroot(sourceRoot.toNio)
      .withPaths(List(file.toNio).asJava)
      .withRules(List("Infertypes").asJava)
      .evaluate()

    evals.getFileEvaluations.headOption match {
      case Some(eval) => Success(eval)
      case None => Failure(new Exception(
        s"""|scalafix failed evaluating $file with error:
            |${evals.getMessageError.asScala.getOrElse("")}.
            |Code error:  ${evals.getErrors.toList}""".stripMargin))
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