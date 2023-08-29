package migrate.internal

import scala.annotation.tailrec
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal

import migrate.interfaces.Logger
import migrate.interfaces.Scala3Compiler
import migrate.utils.Timer._
import scalafix.interfaces.ScalafixPatch

/**
 * Given a [[FileMigrationState]] and a [[Scala3Compiler]], the [[FileMigration]] class tries to find the minimum set of
 * patches that makes the code compile
 */
private[migrate] class FileMigration(
  fileToMigrate: FileMigrationState.Initial,
  compiler: Scala3Compiler,
  logger: Logger) {

  def migrate(): Try[FileMigrationState.FinalState] = {
    val initialState = CompilingState(fileToMigrate.patches, Seq.empty)

    timeAndLog(loopUntilNoCandidates(Success(initialState))) {
      case (timeMs, Success(finalState)) =>
        logger.info(
          s"Found ${finalState.necessaryPatches.size} required patch(es) in ${fileToMigrate.source} after $timeMs ms"
        )
      case (timeMs, Failure(e)) =>
        logger.info(
          s"Failed finding the required patches in ${fileToMigrate.source} after $timeMs ms because ${e.getMessage()}"
        )
    }.map(finalState => fileToMigrate.success(finalState.necessaryPatches))
  }

  @tailrec
  private def loopUntilNoCandidates(state: Try[CompilingState]): Try[CompilingState] =
    state match {
      case Success(state) if state.candidates.nonEmpty =>
        logger.info(s"${state.candidates.size} remaining candidate(s)")
        loopUntilNoCandidates(state.next())
      case finalState => finalState
    }

  /**
   * A instance of [[CompilingState]] is a set of patches that are sufficient to make the code compiles.
   *
   * @param candidates
   *   A set of patches that may or may not be necessary
   * @param necessaryPatches
   *   A set of necessary patches
   */
  private case class CompilingState(candidates: Seq[ScalafixPatch], necessaryPatches: Seq[ScalafixPatch]) {

    def next(): Try[CompilingState] = {
      // We first try to remove all candidates
      val initialStep = CompilationStep(kept = Seq.empty, removed = candidates, necessary = None)

      loopUntilCompile(Success(initialStep)).map { case CompilationStep(kept, _, necessary) =>
        CompilingState(kept, necessaryPatches ++ necessary)
      }
    }

    @tailrec
    private def loopUntilCompile(step: Try[CompilationStep]): Try[CompilationStep] =
      step match {
        case Success(step) =>
          step.doesCompile() match {
            case Success(true) => Success(step)
            case Success(false) =>
              if (step.removed.size == 1) {
                // the last patch is necessary
                Success(CompilationStep(step.kept, Seq.empty, Some(step.removed.head)))
              } else {
                loopUntilCompile(Success(step.keepMorePatches()))
              }
            case Failure(cause) => Failure(cause)
          }
        case failure => failure
      }

    /**
     * A [[CompilationStep]] is an intermediate step at which we try to compile the code
     *
     * @param kept
     *   The patches that we keep to make the code compile
     * @param removed
     *   The patches that we try to remove
     * @param necessary
     *   Some patch that is necessary or none
     */
    private case class CompilationStep(
      kept: Seq[ScalafixPatch],
      removed: Seq[ScalafixPatch],
      necessary: Option[ScalafixPatch]
    ) {

      def doesCompile(): Try[Boolean] =
        fileToMigrate.previewPatches(kept ++ necessaryPatches).map { source =>
          try {
            compiler.quietCompile(List(source))
            true
          } catch {
            case NonFatal(_) =>
              false
          }
        }

      def keepMorePatches(): CompilationStep = {
        val (keepMore, removeLess) = removed.splitAt(removed.size / 2)
        CompilationStep(kept ++ keepMore, removeLess, necessary)
      }
    }
  }
}
