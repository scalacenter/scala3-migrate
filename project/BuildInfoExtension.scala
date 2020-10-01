import sbtbuildinfo.BuildInfoKey._
import sbt._
import sbt.Def.Classpath
import java.io.File

object BuildInfoExtension {
  def fromClasspath(key: String, task: Entry[Classpath]): Entry[String] = {
    map(task) { case(_, classpath) =>
      key -> classpath.map(_.data).mkString(File.pathSeparator) 
    }
  }

  def fromScalacOptions(key: String, task: Entry[Seq[String]]): Entry[Seq[String]] = {
    map(task) { case (_, scalacOptions) => 
      key -> scalacOptions
    }
  }
}