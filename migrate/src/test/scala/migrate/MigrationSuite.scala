package migrate

import migrate.test.BuildInfo
import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit.DiffAssertions
import utils.FileUtils._
import dotty.tools.io.ClassPath


class MigrationSuite extends AnyFunSuiteLike with DiffAssertions {

  val input = AbsolutePath.from(BuildInfo.input)
  val output = AbsolutePath.from(BuildInfo.output)
  val sourceRoot = AbsolutePath.from(BuildInfo.sourceRoot)
  val scala2Classpath = Classpath.from(BuildInfo.scala2Classpath).get
  val scala2CompilerOptions = BuildInfo.scala2CompilerOptions.toSeq
  val toolClasspath = Classpath.from(BuildInfo.toolClasspath).get
  val scala3Classpath = Classpath.from(BuildInfo.scala3Classpath).get
  val scala3CompilerOptions = BuildInfo.scala3CompilerOptions.toSeq
  val scala3ClassDirectory = AbsolutePath.from(BuildInfo.scala3ClassDirectory)
  
  listFiles(input).foreach { inputFile =>
    test(s"${inputFile.getName}") {

      val preview = Main.previewMigration(
        sourceRoot,
        inputFile,
        scala2Classpath,
        scala2CompilerOptions,
        toolClasspath,
        scala3Classpath,
        scala3CompilerOptions,
        scala3ClassDirectory
      ).get

      val relative = inputFile.relativize(input).get
      val outputFile = output.child(relative)
      val outputContent = read(outputFile)
      
      assertNoDiff(preview, outputContent)
    }
  }

  private def getProperty(key: String): String = {
    val props = new java.util.Properties()
    val path = "migrate.properties"
    val in = this.getClass.getClassLoader.getResourceAsStream(path)
    if (in == null) {
      sys.error(s"Failed to load resource $path")
    } else {
      try props.load(in)
      finally in.close()
    }
    props.getProperty(key)
  }
}
