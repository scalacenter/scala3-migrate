package utils

import java.io.File

import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.Settings
import scala.tools.nsc.interactive.Global
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import scala.meta.Term
import scala.meta.internal.proxy.GlobalProxyService
import scala.meta.io.AbsolutePath
import scala.meta.tokens.Token

object CompilerService {

  def newGlobal(cp: List[AbsolutePath], options: List[String]): Try[Settings] = {
    val classpath = cp.mkString(File.pathSeparator)
    val vd        = new VirtualDirectory("(memory)", None)
    val settings  = new Settings()
    settings.Ymacroexpand.value = "discard"
    settings.outputDirs.setSingleOutput(vd)
    settings.classpath.value = classpath
    settings.YpresentationAnyThread.value = true

    val (isSuccess, unprocessed) = settings.processArguments(options, processAll = true)
    (isSuccess, unprocessed) match {
      case (true, Nil) => Success(settings)
      case (isSuccess, unprocessed) =>
        Failure(
          new Exception(
            s"newGlobal failed while processing Arguments. " +
              s"Status is $isSuccess, unprocessed arguments are $unprocessed"
          )
        )
    }
  }

  def getContext(name: Token, g: Global)(implicit unit: g.CompilationUnit): Option[g.Context] =
    getContext(name.pos.start, g)

  def getContext(name: Term.Name, g: Global)(implicit unit: g.CompilationUnit): Option[g.Context] =
    getContext(name.pos.start, g)

  private def getContext(position: Int, g: Global)(implicit unit: g.CompilationUnit): Option[g.Context] = {
    val gpos  = unit.position(position)
    val gtree = GlobalProxyService.typedTreeAt(g, gpos)
    Try(g.doLocateContext(gtree.pos)).toOption
  }
}
