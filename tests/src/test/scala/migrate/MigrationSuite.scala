package migrate

import org.scalatest.funsuite.AnyFunSuiteLike
import utils.FileUtils._
import domain._
import buildinfo._
import utils.FileUtils

class MigrationSuite extends AnyFunSuiteLike {

  val input =  AbsolutePath.from(BuildInfo.input)
  val output = AbsolutePath.from(BuildInfo.output)

  def test(input: AbsolutePath, output: AbsolutePath): Unit = {
    val relativePaths = listFiles(input).map(_.relativize(input).get)
    relativePaths.foreach{ relative =>
      test(s"${relative.getName}"){
        val inputFile = input.child(relative)
        assert(Main.compileInScala213(inputFile).isSuccess)

        val outputFile = output.child(relative)
        val result = Main.runScalafix(inputFile)
        assert(result.isSuccess)
        val outputContent = FileUtils.read(outputFile)
        assert(outputContent == result.get)

        assert(Main.compileInDotty(outputFile).isSuccess)

      }
    }

  }

  test(input, output)

}
