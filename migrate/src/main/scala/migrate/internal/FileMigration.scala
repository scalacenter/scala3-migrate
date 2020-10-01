package migrate.internal

import scalafix.interfaces.ScalafixPatch
import interfaces.Scala3Compiler
import scala.util.{Try, Success, Failure}
import scala.util.control.NonFatal
import scala.annotation.tailrec
import migrate.utils.Timer._

/**
 * Given a [[MigrationFile]] and a [[Scala3Compiler]], the [[FileMigration]] class
 * tries to find the minimum set of patches that makes the code compile
 **/
private[migrate] class FileMigration(file: MigrationFile, compiler: Scala3Compiler) {

  def migrate(): FileMigrationResult = {
    val initialState = CompilingState(file.patches, Seq.empty)
    timedMs {
      loopUntilNoCandidates(Success(initialState))
    }  match {
      case (Success(finalState), timeMs) =>
        scribe.info(s"Found ${finalState.necessaryPatches.size} required patch(es) in ${file.source} after $timeMs ms")
        FileMigrationSuccess(finalState.necessaryPatches)
      case (Failure(exception), timeMs) => FileMigrationFailure(exception)
    }
  }
  
  @tailrec
  private def loopUntilNoCandidates(state: Try[CompilingState]): Try[CompilingState] = {
    state match {
      case Success(state) if state.candidates.nonEmpty => 
        scribe.info(s"${state.candidates.size} remaining candidate(s)")
        loopUntilNoCandidates(state.next())
      case finalState => finalState  
    }
  }

  /**
    * A instance of [[CompilingState]] is a set of patches that are sufficient to make the code compiles.
    *
    * @param candidates         A set of patches that may or may not be necessary
    * @param necessaryPatches   A set of necessary patches 
    */
  private case class CompilingState(
    candidates: Seq[ScalafixPatch],
    necessaryPatches: Seq[ScalafixPatch]
  ) {

    def next(): Try[CompilingState] = {
      // We first try to remove all candidates
      val initialStep = CompilationStep(
        kept = Seq.empty,
        removed = candidates,
        necessary = None 
      )
      
      loopUntilCompile(Success(initialStep)) map {
        case CompilationStep(kept, _, necessary) =>
          CompilingState(kept, necessaryPatches ++ necessary)
      }
    }

    @tailrec
    private def loopUntilCompile(step: Try[CompilationStep]): Try[CompilationStep] = {
      step match {
        case Success(step) =>
          step.doesCompile() match {
            case Success(true) => Success(step)
            case Success(false) => 
              if (step.removed.size == 1) {
                // the last patch is necessary
                Success(
                  CompilationStep(
                    step.kept,
                    Seq.empty,
                    Some(step.removed.head)
                  )
                )
              } else {
                loopUntilCompile(Success(step.keepMorePatches()))
              }
            case Failure(cause) => Failure(cause)
          }
        case failure => failure
      }
    }

    /**
      * A [[CompilationStep]] is an intermediate step at which we try to compile the code
      *
      * @param kept       The patches that we keep to make the code compile
      * @param removed    The patches that we try to remove
      * @param necessary  Some patch that is necessary or none
      */
    private case class CompilationStep(
      kept: Seq[ScalafixPatch],
      removed: Seq[ScalafixPatch],
      necessary: Option[ScalafixPatch]
    ) {

      def doesCompile(): Try[Boolean] = {
        file.previewPatches(kept ++ necessaryPatches)
          .map { source =>
            try {
              compiler.compile(List(source))
              true
            } catch {
              case NonFatal(_) => 
                false
            }
          }
      }

      def keepMorePatches(): CompilationStep = {
        val (keepMore, removeLess) = removed.splitAt(removed.size / 2)
        CompilationStep(
          kept ++ keepMore,
          removeLess,
          necessary
        )
      }
    }
  }
}
