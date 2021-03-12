package migrate.utils

import migrate.Lib213
import migrate.LibToMigrate.CrossVersion
import migrate.LibToMigrate.Revision
import org.scalatest.funsuite.AnyFunSuiteLike
import scalafix.testkit.DiffAssertions

class LibToMigrate() extends AnyFunSuiteLike with DiffAssertions {
  val binary: CrossVersion.Binary         = CrossVersion.Binary("", "")
  val scalafix: Lib213                    = Lib213.from("ch.epfl.scala:scalafix-core:0.9.24", binary).get
  val zioTests: Lib213                    = Lib213.from("dev.zio:zio-test:1.0.0-RC20", binary).get
  val zioTestsNonExistingRevision: Lib213 = Lib213.from("dev.zio:zio-test:2.0.5", binary).get
  test("revision ordering") {
    val libs      = CoursierHelper.getCompatibleForBinary213(scalafix)
    val revisions = libs.map(_.revision).take(2)
    val expected  = Seq(Revision("0.9.24"), Revision("0.9.25"))
    assert(expected == revisions)
  }
  test("wrong revision ordering") {
    val libs      = CoursierHelper.getCompatibleForBinary213(zioTests)
    val revisions = libs.map(_.revision).take(5)
    val expected = List(
      Revision("1.0.0-RC20"),
      Revision("1.0.0-RC21"),
      Revision("1.0.0-RC21-1"),
      Revision("1.0.0-RC21-2"),
      Revision("1.0.0")
    )
    assert(expected == revisions)
  }
}
