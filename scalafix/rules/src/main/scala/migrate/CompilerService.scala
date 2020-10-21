package migrate

import java.io.File

import scala.meta.{Term, Tree}
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

  def getContext(name: Term.Name, g: ScalafixGlobal)(implicit unit: g.CompilationUnit): Option[g.Context] = {
    val gpos = unit.position(name.pos.start)
    GlobalProxy.typedTreeAt(g, gpos)
    Try(g.doLocateContext(gpos)).toOption
  }

  def getTreeInGlobal(name: Token, g: ScalafixGlobal, replace: String)(implicit unit: g.CompilationUnit): Option[g.Tree] = {
    val gpos = unit.position(name.pos.start)
    val gtree = GlobalProxy.typedTreeAt(g, gpos)
    val termName: Option[g.Name] = gtree match {
      case g.Select(_, name) => Some(name)
      case _ => None
    }
    val ftermName = if (replace == ".apply") Some(g.TermName("apply")) else termName
    val context = g.doLocateContext(gtree.pos)
    val gtree2 = context.tree match {
      case apply: g.Apply if apply.fun.isInstanceOf[g.TypeApply] =>
        Option(context.tree.asInstanceOf[g.Tree])
      case _ =>
        val gtree2 = GlobalProxy.typedTreeAt(g, context.tree.pos)
        Option(gtree2.asInstanceOf[g.Tree])
    }
    ftermName.flatMap { termName =>
      gtree2.flatMap(gt => getTheInterstingPartOfTree(g)(gt, termName, replace))
    }
  }

  private def getTheInterstingPartOfTree(g: ScalafixGlobal)(gtree: g.Tree, termName: g.Name, replace: String): Option[g.Tree] = {
    val k = gtree.collect {
      case t @g.TypeApply(fun, args) if fun.isInstanceOf[g.Select] && fun.asInstanceOf[g.Select].name == termName =>
        t
    }
    k.headOption
  }

}
