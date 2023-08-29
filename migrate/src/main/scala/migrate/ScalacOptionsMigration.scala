package migrate

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

import migrate.interfaces.MigratedScalacOptions
import migrate.internal.CommandLineParser
import migrate.internal.MigratedScalacOption
import migrate.internal.MigratedScalacOption._

object ScalacOptionsMigration {
  def migrate(scalacOptions: Seq[String]): MigratedScalacOptions = {
    val migrated = sanitize(scalacOptions).map(migrate)
    val valid    = migrated.collect { case x: Valid => x.value }.toArray
    val renamed  = migrated.collect { case x: Renamed => x.scala2Value -> x.scala3Value }.toMap
    val removed  = migrated.collect { case x: Removed => x.value }.toArray
    val unknown  = migrated.collect { case x: Unknown => x.value }.toArray
    new MigratedScalacOptions(valid.toArray, renamed.asJava, removed.toArray, unknown.toArray)
  }

  private[migrate] def sanitize(initial: Seq[String]): Seq[String] = {
    val line   = initial.mkString(" ")
    val tokens = CommandLineParser.tokenize(line) // could throw an exception

    @tailrec
    def loop(args: List[String], res: List[String]): List[String] =
      args match {
        case Nil => res.reverse
        case first :: second :: tail if first.startsWith("-") && !second.startsWith("-") =>
          loop(tail, (s"$first $second") :: res)

        case first :: tail => loop(tail, first :: res)
      }
    loop(tokens, Nil)
  }

  private def migrate(scalacOption: String): MigratedScalacOption =
    scalacOption match {
      case s"-J$_"                                                => Valid(scalacOption)
      case s"-D$_"                                                => Valid(scalacOption)
      case s"-bootclasspath$_" | s"--boot-class-path$_"           => Valid(scalacOption)
      case s"-classpath$_" | s"-cp$_" | s"--class-path$_"         => Valid(scalacOption)
      case s"-d$_"                                                => Valid(scalacOption)
      case "-deprecation" | "--deprecation"                       => Valid(scalacOption)
      case s"-encoding$_" | s"--encoding$_"                       => Valid(scalacOption)
      case "-explaintypes" | "--explain-types" | "-explain-types" => Renamed(scalacOption, "-explain")
      case "-explain" | "--explain"                               => Valid(scalacOption)
      case s"-extdirs$_" | s"-extension-directories$_"            => Valid(scalacOption)
      case "-feature" | "--feature"                               => Valid(scalacOption)
      case "-help" | "--help" | "-h"                              => Valid(scalacOption)
      case s"-javabootclasspath$_" | s"--java-boot-class-path$_"  => Valid(scalacOption)
      case s"-javaextdirs$_" | s"--java-extension-directories$_"  => Valid(scalacOption)
      case s"-language$_" | s"--language$_"                       => Valid(scalacOption)
      case "-nowarn" | "--no-warnings"                            => Valid(scalacOption)
      case "-print"                                               => Valid(scalacOption)
      case s"-release$_"                                          => Valid(scalacOption)
      case s"-Xsource$_"                                          => Removed(scalacOption) // remove the old Scala 2.13 source
      case s"-source$_"                                           => Valid(scalacOption)
      case s"--source$_"                                          => Valid(scalacOption)
      case s"-sourcepath$_" | s"--source-path$_"                  => Valid(scalacOption)
      case s"-target$target"                                      => Renamed(scalacOption, s"-Xunchecked-java-output-version${migrateJavaTarget(target)}")
      case s"-Xunchecked-java-output-version$_"                   => Valid(scalacOption)
      case "-unchecked" | "--unchecked"                           => Valid(scalacOption)
      case "-uniqid" | "--unique-id"                              => Valid(scalacOption)
      case "-usejavacp" | "--use-java-class-path"                 => Valid(scalacOption)
      case "-verbose" | "--verbose"                               => Valid(scalacOption)
      case "-version" | "--version"                               => Valid(scalacOption)
      // advanced settings
      case "-X"                       => Valid(scalacOption)
      case "-Xcheckinit"              => Renamed(scalacOption, "-Ysafe-init")
      case "-Ysafe-init"              => Valid(scalacOption)
      case "-Xmigration"              => Valid(scalacOption)
      case "-Xmixin-force-forwarders" => Valid(scalacOption)
      case "-Xno-forwarders"          => Valid(scalacOption)
      case "-Xprompt"                 => Valid(scalacOption)
      case "-Xverify"                 => Renamed(scalacOption, "-Xverify-signatures")
      case "-Xverify-signatures"      => Valid(scalacOption)
      case "-Vprint-types"            => Renamed(scalacOption, "-Xprint-types")
      case "-Xprint-types"            => Valid(scalacOption)
      case s"-Xmacro-settings$_"      => Valid(scalacOption)
      case s"-Xmain-class$_"          => Valid(scalacOption)

      // Private settings
      case "-Y"                             => Valid(scalacOption)
      case s"-Ycheck$_"                     => Valid(scalacOption)
      case s"-Ydump-classes$_"              => Valid(scalacOption)
      case "-Yno-generic-signatures"        => Valid(scalacOption)
      case "-Yno-imports"                   => Valid(scalacOption)
      case s"-Yimports$_"                   => Valid(scalacOption)
      case "-Yno-predef"                    => Valid(scalacOption)
      case s"-Yprofile-destination$_"       => Valid(scalacOption)
      case s"-Yprofile-enabofile-enabled$_" => Valid(scalacOption)
      case s"-Yresolve-term-conflict$_"     => Valid(scalacOption)
      case s"-Yskip$_"                      => Valid(scalacOption)
      case s"-Ystop-after$_" | "-stop"      => Valid(scalacOption)
      case s"-Ystop-before$_"               => Valid(scalacOption)

      // Scala.js Settings
      case "-P:scalajs:genStaticForwardersForNonTopLevelObjects" |
          "-scalajs-genStaticForwardersForNonTopLevelObjects" =>
        Ignored
      case "-P:scalajs:mapSourceURI" | "-scalajs-mapSourceURI" => Ignored

      // Verbose settings
      case "-V"                          => Valid(scalacOption)
      case s"-Vprint$_"                  => Valid(scalacOption)
      case s"-Xprint$_"                  => Valid(scalacOption)
      case "-Vphases" | "-Xshow-phases"  => Valid(scalacOption)
      case "-Vprofile"                   => Valid(scalacOption)
      case "-Vclasspath"                 => Renamed(scalacOption, "-Ylog-classpath")
      case "-Ylog-classpath"             => Valid(scalacOption)
      case s"-Vlog$phases"               => Renamed(scalacOption, s"-Ylog$phases")
      case s"-Ylog$_"                    => Valid(scalacOption)
      case "-Vdebug"                     => Renamed(scalacOption, "-Ydebug")
      case "-Ydebug"                     => Valid(scalacOption)
      case "-Vdebug-type-error"          => Renamed(scalacOption, "-Ydebug-type-error")
      case "-Ydebug-type-error"          => Valid(scalacOption)
      case "-Vprint-pos" | "-Xprint-pos" => Renamed(scalacOption, "-Yprint-pos")
      case "-Yprint-pos"                 => Valid(scalacOption)

      // Warning settings
      case "-W"                           => Valid(scalacOption)
      case "-Werror" | "-Xfatal-warnings" => Valid(scalacOption)
      case "-Wvalue-discard"              => Valid(scalacOption)
      case s"-Wunused$_"                  => Valid(scalacOption)
      case s"-Ywarn-unused$unused"        => Renamed(scalacOption, s"-Wunused$unused")
      case s"-Wconf$_"                    => Valid(scalacOption)
      case "-Xlint:deprecation"           => Renamed(scalacOption, "-deprecation")

      // Specific to scala 2
      case s"-dependencyfile$_"                                    => Removed(scalacOption)
      case s"-g$_"                                                 => Removed(scalacOption)
      case "-no-specialization" | "--no-specialization"            => Removed(scalacOption)
      case s"-nobootcp$_" | s"--no-boot-class-path$_"              => Removed(scalacOption)
      case s"-opt$_"                                               => Removed(scalacOption)
      case s"-opt-inline-from$_"                                   => Removed(scalacOption)
      case s"-opt-warnings$_"                                      => Removed(scalacOption)
      case s"-optimize" | "-optimise"                              => Removed(scalacOption)
      case s"-rootdir$_"                                           => Removed(scalacOption)
      case s"-toolcp$_" | s"--tool-class-path$_"                   => Removed(scalacOption)
      case s"-usemanifestc$_"                                      => Removed(scalacOption)
      case s"-Wdead-code$_" | s"-Ywarn-dead-code$_"                => Removed(scalacOption)
      case s"-Wextra-implicit$_"                                   => Removed(scalacOption)
      case s"-Wmacros$_"                                           => Removed(scalacOption)
      case s"-Ywarn-macros$_"                                      => Removed(scalacOption)
      case s"-Wnumeric-widen$_" | s"-Ywarn-numeric-widen$_"        => Removed(scalacOption)
      case s"-Woctal-literal$_"                                    => Removed(scalacOption)
      case s"-Xlint$_"                                             => Removed(scalacOption)
      case s"-Wself-implicit$_"                                    => Removed(scalacOption)
      case s"-Vbrowse$_" | s"-Ybrowse$_"                           => Removed(scalacOption)
      case "-Vdebug-tasty" | "-Ydebug-tasty"                       => Removed(scalacOption)
      case "-Vdoc" | "-Ydoc-debug"                                 => Removed(scalacOption)
      case "-Vfree-terms" | "-Xlog-free-terms"                     => Removed(scalacOption)
      case "-Vfree-types" | "-Xlog-free-types"                     => Removed(scalacOption)
      case "-Vide" | "-Yide-debug"                                 => Removed(scalacOption)
      case "-Vimplicit-conversions" | "-Xlog-implicit-conversions" => Removed(scalacOption)
      case "-Vimplicits" | "-Xlog-implicits"                       => Removed(scalacOption)
      case "-Vimplicits-verbose-tree"                              => Removed(scalacOption)
      case "-Vimplicits-max-refined"                               => Removed(scalacOption)
      case "-Vtype-diffs"                                          => Removed(scalacOption)
      case s"-Vinline$_" | s"-Yopt-log-inline$_"                   => Removed(scalacOption)
      case s"-Vissue$_" | s"-Yissue-debug$_"                       => Removed(scalacOption)
      case "-Vmacro" | "-Ymacro-debug-verbose"                     => Removed(scalacOption)
      case "-Vmacro-lite" | "-Ymacro-debug-lite"                   => Removed(scalacOption)
      case s"-Vopt$_" | s"-Yopt-trace$_"                           => Removed(scalacOption)
      case "-Vpatmat" | "-Ypatmat-debug"                           => Removed(scalacOption)
      case "-Vpos" | "-Ypos-debug"                                 => Removed(scalacOption)
      case s"-Vprint-args$_" | s"--Xprint-args$_"                  => Removed(scalacOption)
      case s"-Vquasiquote$_" | s"Yquasiquote-debug$_"              => Removed(scalacOption)
      case s"-Vreflective-calls$_" | s"-Xlog-reflective-calls$_"   => Removed(scalacOption)
      case s"-Vreify$_" | s"-Yreify-debug$_"                       => Removed(scalacOption)
      case s"-Vshow$_" | s"-Yshow$_"                               => Removed(scalacOption)
      case s"-Vshow-class$_" | s"-Xshow-object$_"                  => Removed(scalacOption)
      case s"-Vshow-member-pos$_" | s"-Yshow-member-pos$_"         => Removed(scalacOption)
      case s"-Vshow-object$_" | s"-Xshow-object$_"                 => Removed(scalacOption)
      case s"-Vshow-symkinds$_" | s"-Yshow-symkinds$_"             => Removed(scalacOption)
      case s"-Vshow-symowners$_" | s"-Yshow-symowners$_"           => Removed(scalacOption)
      case s"-Vstatistics$_" | s"-Ystatistics$_"                   => Removed(scalacOption)
      case "-Ystatistics-enabled"                                  => Removed(scalacOption)
      case s"-Vhot-statistics$_" | s"-Yhot-statistics$_"           => Removed(scalacOption)
      case s"-Vsymbols$_" | s"-Yshow-syms$_"                       => Removed(scalacOption)
      case s"-Vtyper$_" | s"-Ytyper-debug$_"                       => Removed(scalacOption)
      case s"-Xdev$_"                                              => Removed(scalacOption)
      case "-Xasync"                                               => Removed(scalacOption)
      case s"-Xdisable-assertions$_"                               => Removed(scalacOption)
      case s"-Xelide-below$_"                                      => Removed(scalacOption)
      case s"-Xexperimental$_"                                     => Removed(scalacOption)
      case s"-Xfuture$_"                                           => Removed(scalacOption)
      case s"-Xgenerate-phase-graph$_"                             => Removed(scalacOption)
      case s"-Xjline$_"                                            => Removed(scalacOption)
      case s"-Xmaxerrs$_"                                          => Removed(scalacOption)
      case s"-Xmaxwarns$_"                                         => Removed(scalacOption)
      case s"-Xno-patmat-analysis$_"                               => Removed(scalacOption)
      case "-Xnon-strict-patmat-analysis"                          => Removed(scalacOption)
      case s"-Xnojline$_"                                          => Removed(scalacOption)
      case s"-Xreporter$_"                                         => Removed(scalacOption)
      case s"-Xresident$_"                                         => Removed(scalacOption)
      case s"-Xscript$_"                                           => Removed(scalacOption)
      case s"-Xsource-reader$_"                                    => Removed(scalacOption)
      case s"-Xxml$_"                                              => Removed(scalacOption)

      case "-Ytasty-no-annotations"                               => Removed(scalacOption)
      case "-Ytasty-reader"                                       => Removed(scalacOption)
      case "-Ybackend-parallelism"                                => Removed(scalacOption)
      case "-Ybackend-worker-queue"                               => Removed(scalacOption)
      case "-Ybreak-cycles"                                       => Removed(scalacOption)
      case "-Ycache-macro-class-loader"                           => Removed(scalacOption)
      case "-Ycache-plugin-class-loader"                          => Removed(scalacOption)
      case "-Ycompact-trees"                                      => Removed(scalacOption)
      case "-Ydelambdafy"                                         => Removed(scalacOption)
      case "-Yshow-trees"                                         => Removed(scalacOption)
      case "-Yshow-trees-compact"                                 => Removed(scalacOption)
      case "-Yshow-trees-stringified"                             => Removed(scalacOption)
      case "-Ygen-asmp"                                           => Removed(scalacOption)
      case "-Yjar-compression-level"                              => Removed(scalacOption)
      case "-YjarFactory"                                         => Removed(scalacOption)
      case "-Ypickle-java"                                        => Removed(scalacOption)
      case "-Ypickle-write"                                       => Removed(scalacOption)
      case "-Ypickle-write-api-only"                              => Removed(scalacOption)
      case "-Ytrack-dependencies"                                 => Removed(scalacOption)
      case "-Yscala3-implicit-resolution"                         => Removed(scalacOption)
      case s"-Ycache-$_-class-loader"                             => Removed(scalacOption)
      case "-Ymacro-annotations"                                  => Removed(scalacOption)
      case "-Ymacro-classpath"                                    => Removed(scalacOption)
      case "-Youtline"                                            => Removed(scalacOption)
      case "-Ymacro-expand"                                       => Removed(scalacOption)
      case "-Ymacro-global-fresh-names"                           => Removed(scalacOption)
      case "-Yno-completion"                                      => Removed(scalacOption)
      case "-Yno-flat-classpath-cache" | "-YdisableFlatCpCaching" => Removed(scalacOption)
      case "-Yforce-flat-cp-cache"                                => Removed(scalacOption)
      case s"-opt-inline-from$_"                                  => Removed(scalacOption)
      case s"-Yopt-inline-heuristics$_"                           => Removed(scalacOption)
      case s"-Wopt$_"                                             => Removed(scalacOption)
      case s"-Ypatmat-exhaust-depth$_"                            => Removed(scalacOption)
      case "-Ypresentation-any-thread"                            => Removed(scalacOption)
      case "-Ypresentation-debug"                                 => Removed(scalacOption)
      case s"-Ypresentation-delay$_"                              => Removed(scalacOption)
      case "-Ypresentation-locate-source-file"                    => Removed(scalacOption)
      case s"-Ypresentation-log$_"                                => Removed(scalacOption)
      case s"-Ypresentation-replay$_"                             => Removed(scalacOption)
      case "-Ypresentation-strict"                                => Removed(scalacOption)
      case "-Ypresentation-verbose"                               => Removed(scalacOption)
      case "-Yprint-trees"                                        => Removed(scalacOption)
      case "-Yprofile-trace"                                      => Removed(scalacOption)
      case "-Yrangepos"                                           => Removed(scalacOption)
      case "-Yrecursion"                                          => Removed(scalacOption)
      case "-Yreify-copypaste"                                    => Removed(scalacOption)
      case "-Yrepl-class-based"                                   => Removed(scalacOption)
      case "-Yrepl-outdir"                                        => Removed(scalacOption)
      case "-Yrepl-use-magic-imports"                             => Removed(scalacOption)
      case "-Yscriptrunner"                                       => Removed(scalacOption)
      case "-Yvalidate-pos"                                       => Removed(scalacOption)

      // Scala 3 only
      case s"-java-output-version$_"            => Valid(scalacOption)
      case s"-color$_" | s"--color$_"           => Valid(scalacOption)
      case s"-doc-snapshot$_"                   => Valid(scalacOption)
      case "-from-tasty" | "--from-tasty"       => Valid(scalacOption)
      case s"-indent$_"                         => Valid(scalacOption)
      case s"-new-syntax$_"                     => Valid(scalacOption)
      case "-no-indent" | "-noindent"           => Valid(scalacOption)
      case s"-old-syntax$_"                     => Valid(scalacOption)
      case s"-pagewidth$_" | s"--page-width$_"  => Valid(scalacOption)
      case "-print-lines" | "--print-lines"     => Valid(scalacOption)
      case "-print-tasty" | "--print-tasty"     => Valid(scalacOption)
      case s"-project$_"                        => Valid(scalacOption)
      case s"-project-logo$_"                   => Valid(scalacOption)
      case s"-project-url$_"                    => Valid(scalacOption)
      case s"-project-version$_"                => Valid(scalacOption)
      case "-rewrite" | "--rewrite"             => Valid(scalacOption)
      case s"-semanticdb-target$_"              => Valid(scalacOption)
      case s"-siteroot$_"                       => Valid(scalacOption)
      case s"-sourceroot$_"                     => Valid(scalacOption)
      case s"-Vprofile-sorted-by$_"             => Valid(scalacOption)
      case s"-Vprofile-details$_"               => Valid(scalacOption)
      case s"-Vrepl-max-print-elements$_"       => Valid(scalacOption)
      case s"-Vrepl-max-print-characters$_"     => Valid(scalacOption)
      case s"-Xignore-scala2-macros$_"          => Valid(scalacOption)
      case s"-Ximport-suggestion-timeout$_"     => Valid(scalacOption)
      case s"-Xmax-inlined-trees$_"             => Valid(scalacOption)
      case s"-Xmax-inlines$_"                   => Valid(scalacOption)
      case s"-Xprint-diff$_"                    => Valid(scalacOption)
      case s"-Xprint-diff-del$_"                => Valid(scalacOption)
      case s"-Xprint-inline$_"                  => Valid(scalacOption)
      case s"-Xprint-suspension$_"              => Valid(scalacOption)
      case s"-Xrepl-disable-display$_"          => Valid(scalacOption)
      case s"-Xwiki-syntax$_"                   => Valid(scalacOption)
      case s"-Ximplicit-search-limit$_"         => Valid(scalacOption)
      case s"-Ycheck-all-patmat$_"              => Valid(scalacOption)
      case s"-Ycheck-mods$_"                    => Valid(scalacOption)
      case s"-Ycheck-reentrant$_"               => Valid(scalacOption)
      case "-Ycook-comments" | "-Ycook-docs"    => Valid(scalacOption)
      case "-Yread-docs"                        => Valid(scalacOption)
      case s"-Ydebug-error$_"                   => Valid(scalacOption)
      case "-Ydebug-unpickling"                 => Valid(scalacOption)
      case s"-Ydebug-flags$_"                   => Valid(scalacOption)
      case s"-Ydebug-missing-refs$_"            => Valid(scalacOption)
      case s"-Ydebug-names$_"                   => Valid(scalacOption)
      case s"-Ydebug-pos$_"                     => Valid(scalacOption)
      case s"-Ydebug-trace$_"                   => Valid(scalacOption)
      case s"-Ydebug-tree-with-id$_"            => Valid(scalacOption)
      case s"-Ydetailed-stats$_"                => Valid(scalacOption)
      case s"-YdisableFlatCpCaching$_"          => Valid(scalacOption)
      case "-Ydrop-comments" | "-Ydrop-docs"    => Valid(scalacOption)
      case s"-Ydump-sbt-inc$_"                  => Valid(scalacOption)
      case s"-Yerased-terms$_"                  => Valid(scalacOption)
      case s"-Yexplain-lowlevel$_"              => Valid(scalacOption)
      case s"-Yexplicit-nulls$_"                => Valid(scalacOption)
      case s"-Yforce-sbt-phases$_"              => Valid(scalacOption)
      case s"-Yfrom-tasty-ignore-list$_"        => Valid(scalacOption)
      case "-Yno-experimental"                  => Valid(scalacOption)
      case "-Ylegacy-lazy-vals"                 => Valid(scalacOption)
      case s"-Yindent-colons$_"                 => Valid(scalacOption)
      case s"-Yinstrument$_"                    => Valid(scalacOption)
      case s"-Yinstrument-defs$_"               => Valid(scalacOption)
      case s"-Yno-decode-stacktraces$_"         => Valid(scalacOption)
      case s"-Yno-deep-subtypes$_"              => Valid(scalacOption)
      case s"-Yno-double-bindings$_"            => Valid(scalacOption)
      case s"-Yno-kind-polymorphism$_"          => Valid(scalacOption)
      case s"-Yno-patmat-opt$_"                 => Valid(scalacOption)
      case s"-Yplain-printer$_"                 => Valid(scalacOption)
      case s"-Yprint-debug$_"                   => Valid(scalacOption)
      case s"-Yprint-debug-owners$_"            => Valid(scalacOption)
      case "-YprintLevel"                       => Valid(scalacOption)
      case s"-Yprint-pos$_"                     => Valid(scalacOption)
      case s"-Yprint-pos-syms$_"                => Valid(scalacOption)
      case s"-Yprint-syms$_"                    => Valid(scalacOption)
      case s"-Yrequire-targetName$_"            => Valid(scalacOption)
      case "-Yrecheck-test"                     => Valid(scalacOption)
      case "-Ycc-debug"                         => Valid(scalacOption)
      case "-Ycc-no-abbrev"                     => Valid(scalacOption)
      case s"-Yretain-trees$_"                  => Valid(scalacOption)
      case s"-Yscala2-unpickler$_"              => Valid(scalacOption)
      case s"-Yshow-print-errors$_"             => Valid(scalacOption)
      case s"-Yshow-suppressed-errors$_"        => Valid(scalacOption)
      case s"-Yshow-tree-ids$_"                 => Valid(scalacOption)
      case s"-Yshow-var-bounds$_"               => Valid(scalacOption)
      case s"-Ytest-pickler$_"                  => Valid(scalacOption)
      case s"-Yunsound-match-types$_"           => Valid(scalacOption)
      case "-Xcheck-macros" | "--Xcheck-macros" => Valid(scalacOption)
      case "-Xsemanticdb" | "-Ysemanticdb"      => Ignored
      case "-Ykind-projector"                   => Ignored
      case s"-coverage-out$_"                   => Ignored
      case s"--coverage-out$_"                  => Ignored

      // plugin specific scalacOption
      case s"-P$_"               => Ignored
      case s"-Xplugin$_"         => Ignored
      case s"-Xplugin-disable$_" => Ignored
      case s"-Xplugin-list"      => Ignored
      case s"-Xplugin-require$_" => Ignored
      case s"-Xpluginsdir$_"     => Ignored

      case unknown => Unknown(unknown)
    }

  private[migrate] def migrateJavaTarget(target: String): String =
    target match {
      case s"${sep}jvm-1.$v" => sep + v
      case s"${sep}jvm-$v"   => sep + v
      case s"${sep}1.$v"     => sep + v
      case in                => in
    }
}
