package migrate

import java.io.File

import scalafix.v1.SemanticDocument

import scala.meta.Term
import scala.meta.internal.pc.ScalafixGlobal
import scala.meta.internal.proxy.GlobalProxy
import scala.meta.io.AbsolutePath
import scala.meta.tokens.Token
import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.Settings
import scala.util.{Failure, Success, Try}

object CompilerService {

  def newGlobal(cp: List[AbsolutePath], options: List[String]): Try[Settings] = {
    val classpath = cp.mkString(File.pathSeparator)
    val vd = new VirtualDirectory("(memory)", None)
    val settings = new Settings()
    settings.Ymacroexpand.value = "discard"
    settings.outputDirs.setSingleOutput(vd)
    settings.classpath.value = classpath
    settings.YpresentationAnyThread.value = true

    val (isSuccess, unprocessed) = settings.processArguments(options, processAll = true)
    (isSuccess, unprocessed) match {
      case (true, Nil) => Success(settings)
      case (isSuccess, unprocessed) => Failure(new Exception(s"newGlobal failed while processing Arguments. " +
        s"Status is $isSuccess, unprocessed arguments are $unprocessed"))
    }
  }

  def getContext(name: Term.Name, global: ScalafixGlobal)(implicit unit: global.CompilationUnit): Option[global.Context] = {
    val gpos = unit.position(name.pos.start)
    GlobalProxy.typedTreeAt(global, gpos)
    Try(global.doLocateContext(gpos)).toOption
  }

  def getTreeInGlobal(name: Token, global: ScalafixGlobal)(implicit unit: global.CompilationUnit): Option[global.Tree] = {
    val gpos = unit.position(name.pos.start)
    val gtree = GlobalProxy.typedTreeAt(global, gpos)
    val context = global.doLocateContext(gtree.pos)
    val gtree2 = GlobalProxy.typedTreeAt(global, context.tree.pos)
    Option(gtree2.asInstanceOf[global.Tree])
  }

}
