package utils

import java.io.File

import scala.reflect.internal.util.{ Position => ReflectPos }
import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.Settings
import scala.tools.nsc.interactive.Global
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import scala.meta.Position
import scala.meta.Term
import scala.meta.Tree
import scala.meta.internal.proxy.GlobalProxyService
import scala.meta.io.AbsolutePath

import scalafix.v1.SemanticDocument

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
}

class CompilerService[G <: Global](val g: G, doc: SemanticDocument) {
  private lazy val unit: G#CompilationUnit = g.newCompilationUnit(doc.input.text, doc.input.syntax)

  def getContext(name: Term): Option[g.Context] = {
    val gpos = unit.position(name.pos.start)
    getContext(gpos)
  }

  def getContext(tree: Tree): Option[g.Context] = {
    val gpos = getExactPos(tree.pos)
    getContext(gpos)
  }

  def getGlobalTree(tree: Tree): Option[G#Tree] = {
    val gpos  = getPosAfter(tree.pos)
    val gtree = Try(GlobalProxyService.typedTreeAt(g, gpos)).toOption
    if (gtree.isDefined && gtree.get.isInstanceOf[g.Template]) {
      val gpos = getPosBefore(tree.pos)
      Try(GlobalProxyService.typedTreeAt(g, gpos)).toOption
    } else gtree
  }

  private def getContext(gpos: ReflectPos): Option[g.Context] = {
    val gtree = GlobalProxyService.typedTreeAt(g, gpos)
    Try(g.doLocateContext(gtree.pos)).toOption
  }

  private def getPosAfter(scalaMetaPos: Position): ReflectPos =
    ReflectPos.range(unit.source, scalaMetaPos.start, scalaMetaPos.start, scalaMetaPos.end + 1)

  private def getPosBefore(scalaMetaPos: Position): ReflectPos =
    ReflectPos.range(unit.source, scalaMetaPos.start - 1, scalaMetaPos.start, scalaMetaPos.end)

  private def getExactPos(scalaMetaPos: Position): ReflectPos =
    ReflectPos.range(unit.source, scalaMetaPos.start, scalaMetaPos.start, scalaMetaPos.end)

}
