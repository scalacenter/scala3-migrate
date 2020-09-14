package migrate

import org.scalatest.funsuite.AnyFunSuiteLike
import java.io.File
import utils._

class MigrationSuite extends AnyFunSuiteLike {

  val input =  AbsolutePath("/Users/meriamlachkar/scalacenter/scala-migrat3/input/src")
  val output = AbsolutePath("/Users/meriamlachkar/scalacenter/scala-migrat3/output/src")

  val fileInInput = listFiles(input)
  val fileInOutput = listFiles(output)


  def test(input: AbsolutePath, output: AbsolutePath): Unit = {
    val relativePaths = listFiles(input).map(_.relativize(input).get)
    relativePaths.foreach{ relative =>
      test(s"${relative.getName}"){
        val file = input.child(relative)
        Main.compileInScala213(file)
        Main.compileInDotty(file)
        Main.runScalafix(file)
      }
    }

  }
  test(input, output)

  final def listFiles(base: AbsolutePath): Seq[AbsolutePath] = {
    listFiles(base.toFile, true).map(AbsolutePath.from(_).get)
  }

  private def listFiles(base: File, recursive: Boolean = true): Seq[File] = {
    val files = base.listFiles
    val result = files.filter(_.isFile)
    result ++
      files
        .filter(_.isDirectory)
        .filter(_ => recursive)
        .flatMap(listFiles(_, recursive))
  }


}
