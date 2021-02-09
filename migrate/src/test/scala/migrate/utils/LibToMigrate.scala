package migrate.utils

import scala.util.Random

import migrate.Lib213
import migrate.LibToMigrate.CrossVersion
import migrate.LibToMigrate.Revision
import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit.DiffAssertions

class LibToMigrate() extends AnyFunSuiteLike with DiffAssertions {
  val binary: CrossVersion.Binary = CrossVersion.Binary("", "")
  val scalafix: Lib213            = Lib213.from("ch.epfl.scala:scalafix-core:0.9.24", binary).get

  test("revision ordering") {
    val revisions213 = CoursierHelper.searchRevisionsFor(scalafix, "2.13")
    val shuffled     = Random.shuffle(revisions213)
    assert(revisions213 == shuffled.sorted)
  }
  //TODO FIX - Wrong revision
  test("wrong revision ordering") {
    Revision("1.0.0-RC21-2")
    Revision("1.0.0")
//   assert(Seq(revision1, revision2) ==  Seq(revision1, revision2).sorted)
  }
}
