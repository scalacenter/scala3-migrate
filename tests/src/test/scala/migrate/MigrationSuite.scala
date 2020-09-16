package migrate

import org.scalatest.funsuite.AnyFunSuiteLike
import utils.FileUtils._
import domain._
import buildinfo._

class MigrationSuite extends AnyFunSuiteLike {

  val input =  AbsolutePath.from(BuildInfo.input)
  val output = AbsolutePath.from(BuildInfo.output)

  def test(input: AbsolutePath, output: AbsolutePath): Unit = {
    val relativePaths = listFiles(input).map(_.relativize(input).get)
    relativePaths.foreach{ relative =>
      test(s"${relative.getName}"){
        val file = input.child(relative)
        assert(Main.compileInScala213(file).isSuccess)
        assert(Main.compileInDotty(file).isSuccess)
        assert(Main.runScalafix(file).isSuccess)

      }
    }

  }

  test(input, output)

}
