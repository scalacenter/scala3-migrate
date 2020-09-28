package fix

import java.io.File

import scala.meta.io.AbsolutePath
import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.Settings
import scala.util.{Failure, Success, Try}


object CompilerUtils {

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

}
