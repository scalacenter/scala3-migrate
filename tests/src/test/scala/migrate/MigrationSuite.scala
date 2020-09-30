package migrate

import org.scalatest.funsuite.AnyFunSuiteLike
import utils.FileUtils._
import domain._
import buildinfo._
import scalafix.testkit.DiffAssertions
import utils.FileUtils


class MigrationSuite extends AnyFunSuiteLike with DiffAssertions {

  val input = AbsolutePath.from(BuildInfo.input)
  val output = AbsolutePath.from(BuildInfo.output)

  val migrateClasspath = Classpath.from(getProperty("migrateClasspath")).get

  runTests()

  private def runTests(): Unit = {
    val relativePaths = listFiles(input).map(_.relativize(input).get)
    relativePaths.foreach { relative =>
      test(s"${relative.getName}") {
        val inputFile = input.child(relative)
        assert(Main.compileInScala213(inputFile).isSuccess)

        val outputFile = output.child(relative)
        val result = Main.runScalafix(inputFile, input, migrateClasspath)
        assert(result.isSuccess, result.get)
        val outputContent = FileUtils.read(outputFile)
        assertNoDiff(outputContent, result.get.previewPatches().get)
        val compileInDotty = Main.compileInDotty(outputFile)
        assert(compileInDotty.isSuccess, compileInDotty.get)
      }
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
