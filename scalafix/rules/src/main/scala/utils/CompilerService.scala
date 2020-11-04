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

  def getContext(name: Term, g: Global)(implicit unit: g.CompilationUnit): Option[g.Context] = {
    val gpos = unit.position(name.pos.start)
    getContext(gpos, g)
  }

  def getContext(tree: Tree, g: Global)(implicit unit: g.CompilationUnit): Option[g.Context] = {
    val gpos = getExactPos(tree.pos, g)
    getContext(gpos, g)
  }

  def getGlobalTree(tree: Tree, g: Global)(implicit unit: g.CompilationUnit): Option[g.Tree] = {
    val gpos  = getPosAfter(tree.pos, g)
    val gtree = Try(GlobalProxyService.typedTreeAt(g, gpos)).toOption
    if (gtree.isDefined && gtree.get.isInstanceOf[g.Template]) {
      val gpos = getPosBefore(tree.pos, g)
      Try(GlobalProxyService.typedTreeAt(g, gpos)).toOption
    } else gtree
  }

  private def getContext(gpos: ReflectPos, g: Global): Option[g.Context] = {
    val gtree = GlobalProxyService.typedTreeAt(g, gpos)
    Try(g.doLocateContext(gtree.pos)).toOption
  }

  private def getPosAfter(scalaMetaPos: Position, g: Global)(implicit
    unit: g.CompilationUnit
  ): ReflectPos = ReflectPos.range(unit.source, scalaMetaPos.start, scalaMetaPos.start, scalaMetaPos.end + 1)

  private def getPosBefore(scalaMetaPos: Position, g: Global)(implicit
    unit: g.CompilationUnit
  ): ReflectPos = ReflectPos.range(unit.source, scalaMetaPos.start - 1, scalaMetaPos.start, scalaMetaPos.end)

  private def getExactPos(scalaMetaPos: Position, g: Global)(implicit
    unit: g.CompilationUnit
  ): ReflectPos = ReflectPos.range(unit.source, scalaMetaPos.start, scalaMetaPos.start, scalaMetaPos.end)

}
