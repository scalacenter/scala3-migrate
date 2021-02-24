package migrate

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

import scala.meta.XtensionTokenizeInputLike
import scala.meta.io.AbsolutePath

import org.scalatest.FunSuiteLike
import scalafix.internal.patch.PatchInternals
import scalafix.testkit._

class RuleSuite extends AbstractSemanticRuleSuite with FunSuiteLike {
  val (passing, failing) = testsToRun.partition(!_.path.testName.contains("_fails"))
  passing.foreach(runOn)
//  runSpecificTests("etaExpansion")
//  writeTestResult("etaExpansion")

  //   for running only one test if using Intellij
  def runSpecificTests(name: String): Unit =
    filterRuleTest(name).map(runOn)

  //   for overwriting a result test in output directory
  def writeTestResult(name: String): Unit =
    filterRuleTest(name).map(runAndWriteResult)

  private def filterRuleTest(name: String): List[RuleTest] =
    testsToRun.filter(_.path.testName.toLowerCase.contains(name.toLowerCase()))

  // Write the result directly to output folder, to avoid fixing by hand the diff
  private def runAndWriteResult(ruleTest: RuleTest): Unit = {
    val (rule, sdoc) = ruleTest.run.apply()
    rule.beforeStart()
    val res =
      try rule.semanticPatch(sdoc, suppress = false)
      finally rule.afterComplete()
    // verify to verify that tokenPatchApply and fixed are the same
    val fixed =
      PatchInternals.tokenPatchApply(res.ruleCtx, res.semanticdbIndex, res.patches)
    val tokens        = fixed.tokenize.get
    val _ :: obtained = SemanticRuleSuite.stripTestkitComments(tokens).linesIterator.toList
    ruleTest.path.resolveOutput(props) match {
      case Right(file) => writeFile(file, obtained.mkString("\n"))
      case Left(err)   => throw new Exception(s"File not found $err")
    }
  }

  private def writeFile(path: AbsolutePath, s: String): Unit = {
    val filename = path.toNIO.getFileName.toString
    val parent   = path.toNIO.getParent.toString
    val file     = new File(parent, filename)
    val bw       = new BufferedWriter(new FileWriter(file))
    bw.write(s)
    bw.close()
  }

}
