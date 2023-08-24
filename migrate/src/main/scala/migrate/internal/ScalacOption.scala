package migrate.internal

import scala.annotation.tailrec

trait ScalacOption extends Product with Serializable {
  def toScala3: Option[Scala3cOption] = this match {
    case opt: Scala3cOption => Some(opt)
    case _                  => None
  }
}

sealed trait Scala2cOption extends ScalacOption {
  val scala2Value: String
  lazy val scala2ValueForBuild: Seq[String] = scala2Value.split(" ").toSeq
}

sealed trait Scala3cOption extends ScalacOption {
  val scala3Value: String
  lazy val scala3ValueForBuild: Seq[String] = scala3Value.split(" ").toSeq
}

object ScalacOption {
  def from(s: String): ScalacOption =
    s match {
      case s"-J$flag"                                             => Shared.JvmArg(flag)
      case s"-D$property"                                         => Shared.Property(property)
      case s"-bootclasspath$path"                                 => Shared.Bootclasspath(path)
      case s"--boot-class-path$path"                              => Shared.Bootclasspath(path)
      case s"-classpath$path"                                     => Shared.Classpath(path)
      case s"-cp$path"                                            => Shared.Classpath(path)
      case s"--class-path$path"                                   => Shared.Classpath(path)
      case s"-d$directoryOrJar"                                   => Shared.D(directoryOrJar)
      case "-deprecation" | "--deprecation"                       => Shared.Deprecation
      case s"-encoding$encoding"                                  => Shared.Encoding(encoding)
      case s"--encoding$encoding"                                 => Shared.Encoding(encoding)
      case "-explaintypes" | "--explain-types" | "-explain-types" => Renamed.Explaintypes
      case s"-extdirs$path"                                       => Shared.Extdirs(path)
      case s"-extension-directories$path"                         => Shared.Extdirs(path)
      case "-feature" | "--feature"                               => Shared.Feature
      case "-help" | "--help" | "-h"                              => Shared.Help
      case s"-javabootclasspath$path"                             => Shared.Javabootclasspath(path)
      case s"--java-boot-class-path$path"                         => Shared.Javabootclasspath(path)
      case s"-javaextdirs$path"                                   => Shared.Javaextdirs(path)
      case s"--java-extension-directories$path"                   => Shared.Javaextdirs(path)
      case s"-language$features"                                  => Shared.Language(features)
      case s"--language$features"                                 => Shared.Language(features)
      case "-nowarn" | "--no-warnings"                            => Shared.Nowarn
      case "-print"                                               => Shared.Print
      case s"-release$release"                                    => Shared.Release(release)
      case s"-Xsource$version"                                    => Renamed.Xsource(version)
      case s"-source$version"                                     => Renamed.Xsource(version)
      case s"--source$version"                                    => Renamed.Xsource(version)
      case s"-sourcepath$path"                                    => Shared.SourcePath(path)
      case s"--source-path$path"                                  => Shared.SourcePath(path)
      case s"-target$target"                                      => Renamed.Target(target)
      case "-unchecked" | "--unchecked"                           => Shared.Unchecked
      case "-uniqid" | "--unique-id"                              => Shared.Uniqid
      case "-usejavacp" | "--use-java-class-path"                 => Shared.Usejavacp
      case "-verbose" | "--verbose"                               => Shared.Verbose
      case "-version" | "--version"                               => Shared.Version
      // advanced settings
      case "-X"                               => Shared.XHelp
      case "-Xcheckinit"                      => Renamed.Xcheckinit
      case "-Xmigration"                      => Shared.Xmigration
      case "-Xmixin-force-forwarders"         => Shared.XmixinForceForwarders
      case "-Xno-forwarders"                  => Shared.XnoForwarders
      case "-Xprompt"                         => Shared.Xprompt
      case "-Xverify" | "-Xverify-signatures" => Renamed.Xverify
      case "-Vprint-types" | "-Xprint-types"  => Renamed.VprintTypes
      case s"-Xmacro-settings$settings"       => Shared.XmacroSettings(settings)
      case s"-Xmain-class$main"               => Shared.XmainClass(main)

      // Private settings
      case "-Y"                                    => Shared.YHelp
      case s"-Ycheck$phases"                       => Shared.Ycheck(phases)
      case s"-Ydump-classes$dir"                   => Shared.YdumpClasses(dir)
      case "-Yno-generic-signatures"               => Shared.YnoGenericSignatures
      case "-Yno-imports"                          => Shared.YnoImports
      case s"-Yimports$imports"                    => Shared.Yimports(imports)
      case "-Yno-predef"                           => Shared.YnoPredef
      case s"-Yprofile-destination$file"           => Shared.YprofileDestination(file)
      case s"-Yprofile-enabofile-enabled$strategy" => Shared.YprofileEnabled(strategy)
      case s"-Yresolve-term-conflict$strategy"     => Shared.YresolveTermConflict(strategy)
      case s"-Yskip$phases"                        => Shared.Yskip(phases)
      case s"-Ystop-after$phases"                  => Shared.YstopAfter(phases)
      case s"-Ystop$phases"                        => Shared.YstopAfter(phases)
      case s"-Ystop-before$phases"                 => Shared.YstopBefore(phases)

      // Scala.js Settings
      case "-P:scalajs:genStaticForwardersForNonTopLevelObjects" |
          "-scalajs-genStaticForwardersForNonTopLevelObjects" =>
        Renamed.ScalaJsStatic
      case "-P:scalajs:mapSourceURI" | "-scalajs-mapSourceURI" => Renamed.ScalaJsURI

      // Verbose settings
      case "-V"                                        => Shared.Vhelp
      case s"-Vprint$phases"                           => Shared.Vprint(phases)
      case s"-Xprint$phases"                           => Shared.Vprint(phases)
      case "-Vphases" | "-Xshow-phases"                => Shared.Vphases
      case "-Vprofile"                                 => Shared.Vprofile
      case "-Vclasspath"                               => Renamed.Vclasspath
      case "-Ylog-classpath"                           => Renamed.Vclasspath
      case s"-Vlog$phases"                             => Renamed.Vlog(phases)
      case s"-Ylog$phases"                             => Renamed.Vlog(phases)
      case "-Vdebug" | "-Ydebug"                       => Renamed.Vdebug
      case "-Vdebug-type-error" | "-Ydebug-type-error" => Renamed.VdebugTypeError
      case "-Vprint-pos" | "-Xprint-pos"               => Renamed.VprintPos

      // Warning settings
      case "-W"                           => Shared.Whelp
      case "-Werror" | "-Xfatal-warnings" => Shared.Werror
      case "-Wvalue-discard"              => Shared.WvalueDiscard
      case s"-Wunused$warnings"           => Shared.Wunused(warnings)
      case s"-Wconf$patterns"             => Shared.Wconf(patterns)
      case "-Xlint:deprecation"           => Renamed.XlintDeprecation

      // Specific to scala 2
      case _ if s.startsWith("-dependencyfile")                                         => Scala2Specific(s)
      case _ if s.startsWith("-g")                                                      => Scala2Specific(s)
      case "-no-specialization" | "--no-specialization"                                 => Scala2Specific(s)
      case _ if s.startsWith("-nobootcp") | s.startsWith("--no-boot-class-path")        => Scala2Specific(s)
      case _ if s.startsWith("-opt")                                                    => Scala2Specific(s)
      case _ if s.startsWith("-opt-inline-from")                                        => Scala2Specific(s)
      case _ if s.startsWith("-opt-warnings")                                           => Scala2Specific(s)
      case _ if s.startsWith("-optimize")                                               => Scala2Specific(s)
      case _ if s.startsWith("-rootdir")                                                => Scala2Specific(s)
      case _ if s.startsWith("-toolcp") | s.startsWith("--tool-class-path")             => Scala2Specific(s)
      case _ if s.startsWith("-usemanifestc")                                           => Scala2Specific(s)
      case _ if s.startsWith("-Wdead-code") || s.startsWith("-Ywarn-dead-code")         => Scala2Specific(s)
      case _ if s.startsWith("-Wextra-implicit")                                        => Scala2Specific(s)
      case _ if s.startsWith("-Wmacros")                                                => Scala2Specific(s)
      case _ if s.startsWith("-Wnumeric-widen") || s.startsWith("-Ywarn-numeric-widen") => Scala2Specific(s)
      case _ if s.startsWith("-Woctal-literal")                                         => Scala2Specific(s)
      case _ if s.startsWith("-Xlint")                                                  => Scala2Specific(s)
      case _ if s.startsWith("-Wself-implicit")                                         => Scala2Specific(s)
      case _ if s.startsWith("-Vbrowse") || s.startsWith("-Ybrowse")                    => Scala2Specific(s)
      case "-Vdebug-tasty" | "-Ydebug-tasty"                                            => Scala2Specific(s)
      case "-Vdoc" | "-Ydoc-debug"                                                      => Scala2Specific(s)
      case "-Vfree-terms" | "-Xlog-free-terms"                                          => Scala2Specific(s)
      case "-Vfree-types" | "-Xlog-free-types"                                          => Scala2Specific(s)
      case "-Vide" | "-Yide-debug"                                                      => Scala2Specific(s)
      case "-Vimplicit-conversions" | "-Xlog-implicit-conversions"                      => Scala2Specific(s)
      case "-Vimplicits" | "-Xlog-implicits"                                            => Scala2Specific(s)
      case "-Vimplicits-verbose-tree"                                                   => Scala2Specific(s)
      case "-Vimplicits-max-refined"                                                    => Scala2Specific(s)
      case "-Vtype-diffs"                                                               => Scala2Specific(s)
      case _ if s.startsWith("-Vinline") || s.startsWith("-Yopt-log-inline")            => Scala2Specific(s)
      case _ if s.startsWith("-Vissue") || s.startsWith("-Yissue-debug")                => Scala2Specific(s)
      case "-Vmacro" | "-Ymacro-debug-ver"                                              => Scala2Specific(s)
      case "-Vmacro-lite" | "-Ymacro-debug-lite"                                        => Scala2Specific(s)
      case _ if s.startsWith("-Vopt") || s.startsWith("-Yopt-trace")                    => Scala2Specific(s)
      case "-Vpatmat" | "-Ypatmat-debug"                                                => Scala2Specific(s)
      case "-Vpos" | "-Ypos-debug"                                                      => Scala2Specific(s)
      case _ if s.startsWith("-Vprint-args") || s.startsWith("--Xprint-args")           => Scala2Specific(s)
      case _ if s.startsWith("-Vquasiquote") || s.startsWith("Yquasiquote-debug")       => Scala2Specific(s)
      case _ if s.startsWith("-Vreflective-calls") || s.startsWith("-Xlog-reflectiv")   => Scala2Specific(s)
      case _ if s.startsWith("-Vreify") || s.startsWith("-Yreify-debug")                => Scala2Specific(s)
      case _ if s.startsWith("-Vshow") || s.startsWith("-Yshow")                        => Scala2Specific(s)
      case _ if s.startsWith("-Vshow-class") || s.startsWith("-Xshow-object")           => Scala2Specific(s)
      case _ if s.startsWith("-Vshow-member-pos") || s.startsWith("-Yshow-member-pos")  => Scala2Specific(s)
      case _ if s.startsWith("-Vshow-object") || s.startsWith("-Xshow-object")          => Scala2Specific(s)
      case _ if s.startsWith("-Vshow-symkinds") || s.startsWith("-Yshow-symkinds")      => Scala2Specific(s)
      case _ if s.startsWith("-Vshow-symowners") || s.startsWith("-Yshow-symowners")    => Scala2Specific(s)
      case _ if s.startsWith("-Vstatistics") || s.startsWith("-Ystatistics")            => Scala2Specific(s)
      case "-Ystatistics-enabled"                                                       => Scala2Specific(s)
      case _ if s.startsWith("-Vhot-statistics") || s.startsWith("-Yhot-statistics")    => Scala2Specific(s)
      case _ if s.startsWith("-Vsymbols") || s.startsWith("-Yshow-syms")                => Scala2Specific(s)
      case _ if s.startsWith("-Vtyper") || s.startsWith("-Ytyper-debug")                => Scala2Specific(s)
      case _ if s.startsWith("-Xdev")                                                   => Scala2Specific(s)
      case "-Xasync"                                                                    => Scala2Specific(s)
      case _ if s.startsWith("-Xdisable-assertions")                                    => Scala2Specific(s)
      case _ if s.startsWith("-Xelide-below")                                           => Scala2Specific(s)
      case _ if s.startsWith("-Xexperimental")                                          => Scala2Specific(s)
      case _ if s.startsWith("-Xfuture")                                                => Scala2Specific(s)
      case _ if s.startsWith("-Xgenerate-phase-graph")                                  => Scala2Specific(s)
      case _ if s.startsWith("-Xjline")                                                 => Scala2Specific(s)
      case _ if s.startsWith("-Xmaxerrs")                                               => Scala2Specific(s)
      case _ if s.startsWith("-Xmaxwarns")                                              => Scala2Specific(s)
      case _ if s.startsWith("-Xno-patmat-analysis")                                    => Scala2Specific(s)
      case "-Xnon-strict-patmat-analysis"                                               => Scala2Specific(s)
      case _ if s.startsWith("-Xnojline")                                               => Scala2Specific(s)
      case _ if s.startsWith("-Xreporter")                                              => Scala2Specific(s)
      case _ if s.startsWith("-Xresident")                                              => Scala2Specific(s)
      case _ if s.startsWith("-Xscript")                                                => Scala2Specific(s)
      case _ if s.startsWith("-Xsource-reader")                                         => Scala2Specific(s)
      case _ if s.startsWith("-Xxml")                                                   => Scala2Specific(s)

      case "-Ytasty-no-annotations"                               => Scala2Specific(s)
      case "-Ytasty-reader"                                       => Scala2Specific(s)
      case "-Ybackend-parallelism"                                => Scala2Specific(s)
      case "-Ybackend-worker-queue"                               => Scala2Specific(s)
      case "-Ybreak-cycles"                                       => Scala2Specific(s)
      case "-Ycache-macro-class-loader"                           => Scala2Specific(s)
      case "-Ycache-plugin-class-loader"                          => Scala2Specific(s)
      case "-Ycompact-trees"                                      => Scala2Specific(s)
      case "-Ydelambdafy"                                         => Scala2Specific(s)
      case "-Yshow-trees"                                         => Scala2Specific(s)
      case "-Yshow-trees-compact"                                 => Scala2Specific(s)
      case "-Yshow-trees-stringified"                             => Scala2Specific(s)
      case "-Ygen-asmp"                                           => Scala2Specific(s)
      case "-Yjar-compression-level"                              => Scala2Specific(s)
      case "-YjarFactory"                                         => Scala2Specific(s)
      case "-Ypickle-java"                                        => Scala2Specific(s)
      case "-Ypickle-write"                                       => Scala2Specific(s)
      case "-Ypickle-write-api-only"                              => Scala2Specific(s)
      case "-Ytrack-dependencies"                                 => Scala2Specific(s)
      case "-Yscala3-implicit-resolution"                         => Scala2Specific(s)
      case s"-Ycache-$_-class-loader"                             => Scala2Specific(s)
      case "-Ymacro-annotations"                                  => Scala2Specific(s)
      case "-Ymacro-classpath"                                    => Scala2Specific(s)
      case "-Youtline"                                            => Scala2Specific(s)
      case "-Ymacro-expand"                                       => Scala2Specific(s)
      case "-Ymacro-global-fresh-names"                           => Scala2Specific(s)
      case "-Yno-completion"                                      => Scala2Specific(s)
      case "-Yno-flat-classpath-cache" | "-YdisableFlatCpCaching" => Scala2Specific(s)
      case "-Yforce-flat-cp-cache"                                => Scala2Specific(s)
      case _ if s.startsWith("-opt-inline-from")                  => Scala2Specific(s)
      case _ if s.startsWith("-Yopt-inline-heuristics")           => Scala2Specific(s)
      case _ if s.startsWith("-Wopt")                             => Scala2Specific(s)
      case _ if s.startsWith("-Ypatmat-exhaust-depth")            => Scala2Specific(s)
      case "-Ypresentation-any-thread"                            => Scala2Specific(s)
      case "-Ypresentation-debug"                                 => Scala2Specific(s)
      case _ if s.startsWith("-Ypresentation-delay")              => Scala2Specific(s)
      case "-Ypresentation-locate-source-file"                    => Scala2Specific(s)
      case _ if s.startsWith("-Ypresentation-log")                => Scala2Specific(s)
      case _ if s.startsWith("-Ypresentation-replay")             => Scala2Specific(s)
      case "-Ypresentation-strict"                                => Scala2Specific(s)
      case "-Ypresentation-verbose"                               => Scala2Specific(s)
      case "-Yprint-trees"                                        => Scala2Specific(s)
      case "-Yprofile-trace"                                      => Scala2Specific(s)
      case "-Yrangepos"                                           => Scala2Specific(s)
      case "-Yrecursion"                                          => Scala2Specific(s)
      case "-Yreify-copypaste"                                    => Scala2Specific(s)
      case "-Yrepl-class-based"                                   => Scala2Specific(s)
      case "-Yrepl-outdir"                                        => Scala2Specific(s)
      case "-Yrepl-use-magic-imports"                             => Scala2Specific(s)
      case "-Yscriptrunner"                                       => Scala2Specific(s)
      case "-Yvalidate-pos"                                       => Scala2Specific(s)

      // specific 3
      case _ if s.startsWith("-java-output-version")                      => Scala3Specific(s)
      case _ if s.startsWith("-color") | s.startsWith("--color")          => Scala3Specific(s)
      case _ if s.startsWith("-doc-snapshot")                             => Scala3Specific(s)
      case "-explain" | "--explain"                                       => Scala3Specific(s)
      case "-from-tasty" | "--from-tasty"                                 => Scala3Specific(s)
      case _ if s.startsWith("-indent")                                   => Scala3Specific(s)
      case _ if s.startsWith("-new-syntax")                               => Scala3Specific(s)
      case "-no-indent" | "-noindent"                                     => Scala3Specific(s)
      case _ if s.startsWith("-old-syntax")                               => Scala3Specific(s)
      case _ if s.startsWith("-pagewidth") | s.startsWith("--page-width") => Scala3Specific(s)
      case "-print-lines" | "--print-lines"                               => Scala3Specific(s)
      case "-print-tasty" | "--print-tasty"                               => Scala3Specific(s)
      case _ if s.startsWith("-project")                                  => Scala3Specific(s)
      case _ if s.startsWith("-project-logo")                             => Scala3Specific(s)
      case _ if s.startsWith("-project-url")                              => Scala3Specific(s)
      case _ if s.startsWith("-project-version")                          => Scala3Specific(s)
      case "-rewrite" | "--rewrite"                                       => Scala3Specific(s)
      case _ if s.startsWith("-semanticdb-target")                        => Scala3Specific(s)
      case _ if s.startsWith("-siteroot")                                 => Scala3Specific(s)
      case _ if s.startsWith("-sourceroot")                               => Scala3Specific(s)
      case _ if s.startsWith("-Vprofile-sorted-by")                       => Scala3Specific(s)
      case _ if s.startsWith("-Vprofile-details")                         => Scala3Specific(s)
      case _ if s.startsWith("-Vrepl-max-print-elements")                 => Scala3Specific(s)
      case _ if s.startsWith("-Vrepl-max-print-characters")               => Scala3Specific(s)
      case _ if s.startsWith("-Xignore-scala2-macros")                    => Scala3Specific(s)
      case _ if s.startsWith("-Ximport-suggestion-timeout")               => Scala3Specific(s)
      case _ if s.startsWith("-Xmax-inlined-trees")                       => Scala3Specific(s)
      case _ if s.startsWith("-Xmax-inlines")                             => Scala3Specific(s)
      case _ if s.startsWith("-Xprint-diff")                              => Scala3Specific(s)
      case _ if s.startsWith("-Xprint-diff-del")                          => Scala3Specific(s)
      case _ if s.startsWith("-Xprint-inline")                            => Scala3Specific(s)
      case _ if s.startsWith("-Xprint-suspension")                        => Scala3Specific(s)
      case _ if s.startsWith("-Xrepl-disable-display")                    => Scala3Specific(s)
      case _ if s.startsWith("-Xwiki-syntax")                             => Scala3Specific(s)
      case _ if s.startsWith("-Ximplicit-search-limit")                   => Scala3Specific(s)
      case _ if s.startsWith("-Ycheck-all-patmat")                        => Scala3Specific(s)
      case _ if s.startsWith("-Ycheck-mods")                              => Scala3Specific(s)
      case _ if s.startsWith("-Ycheck-reentrant")                         => Scala3Specific(s)
      case "-Ycook-comments" | "-Ycook-docs"                              => Scala3Specific(s)
      case "-Yread-docs"                                                  => Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-error")                             => Scala3Specific(s)
      case "-Ydebug-unpickling"                                           => Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-flags")                             => Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-missing-refs")                      => Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-names")                             => Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-pos")                               => Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-trace")                             => Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-tree-with-id")                      => Scala3Specific(s)
      case _ if s.startsWith("-Ydetailed-stats")                          => Scala3Specific(s)
      case _ if s.startsWith("-YdisableFlatCpCaching")                    => Scala3Specific(s)
      case "-Ydrop-comments" | "-Ydrop-docs"                              => Scala3Specific(s)
      case _ if s.startsWith("-Ydump-sbt-inc")                            => Scala3Specific(s)
      case _ if s.startsWith("-Yerased-terms")                            => Scala3Specific(s)
      case _ if s.startsWith("-Yexplain-lowlevel")                        => Scala3Specific(s)
      case _ if s.startsWith("-Yexplicit-nulls")                          => Scala3Specific(s)
      case _ if s.startsWith("-Yforce-sbt-phases")                        => Scala3Specific(s)
      case _ if s.startsWith("-Yfrom-tasty-ignore-list")                  => Scala3Specific(s)
      case "-Yno-experimental"                                            => Scala3Specific(s)
      case "-Ylegacy-lazy-vals"                                           => Scala3Specific(s)
      case _ if s.startsWith("-Yindent-colons")                           => Scala3Specific(s)
      case _ if s.startsWith("-Yinstrument")                              => Scala3Specific(s)
      case _ if s.startsWith("-Yinstrument-defs")                         => Scala3Specific(s)
      case _ if s.startsWith("-Yno-decode-stacktraces")                   => Scala3Specific(s)
      case _ if s.startsWith("-Yno-deep-subtypes")                        => Scala3Specific(s)
      case _ if s.startsWith("-Yno-double-bindings")                      => Scala3Specific(s)
      case _ if s.startsWith("-Yno-kind-polymorphism")                    => Scala3Specific(s)
      case _ if s.startsWith("-Yno-patmat-opt")                           => Scala3Specific(s)
      case _ if s.startsWith("-Yplain-printer")                           => Scala3Specific(s)
      case _ if s.startsWith("-Yprint-debug")                             => Scala3Specific(s)
      case _ if s.startsWith("-Yprint-debug-owners")                      => Scala3Specific(s)
      case "-YprintLevel"                                                 => Scala3Specific(s)
      case _ if s.startsWith("-Yprint-pos")                               => Scala3Specific(s)
      case _ if s.startsWith("-Yprint-pos-syms")                          => Scala3Specific(s)
      case _ if s.startsWith("-Yprint-syms")                              => Scala3Specific(s)
      case _ if s.startsWith("-Yrequire-targetName")                      => Scala3Specific(s)
      case "-Yrecheck-test"                                               => Scala3Specific(s)
      case "-Ycc-debug"                                                   => Scala3Specific(s)
      case "-Ycc-no-abbrev"                                               => Scala3Specific(s)
      case _ if s.startsWith("-Yretain-trees")                            => Scala3Specific(s)
      case _ if s.startsWith("-Yscala2-unpickler")                        => Scala3Specific(s)
      case _ if s.startsWith("-Yshow-print-errors")                       => Scala3Specific(s)
      case _ if s.startsWith("-Yshow-suppressed-errors")                  => Scala3Specific(s)
      case _ if s.startsWith("-Yshow-tree-ids")                           => Scala3Specific(s)
      case _ if s.startsWith("-Yshow-var-bounds")                         => Scala3Specific(s)
      case _ if s.startsWith("-Ytest-pickler")                            => Scala3Specific(s)
      case _ if s.startsWith("-Yunsound-match-types")                     => Scala3Specific(s)
      case "-Xcheck-macros" | "--Xcheck-macros"                           => Scala3Specific(s)
      case "-Xsemanticdb" | "-Ysemanticdb"                                => SemanticDB
      case _ if s.startsWith("-Ykind-projector")                          => KindProjector
      case s"-coverage-out$out"                                           => CoverageOut(out)
      case s"--coverage-out$out"                                          => CoverageOut(out)

      // plugin specific scalacOption
      case s"-P$plugin"               => PluginSpecific.Plugin(plugin)
      case s"-Xplugin$paths"          => PluginSpecific.Xplugin(paths)
      case s"-Xplugin-disable$plugin" => PluginSpecific.XpluginDisable(plugin)
      case s"-Xplugin-list"           => PluginSpecific.XpluginList
      case s"-Xplugin-require$plugin" => PluginSpecific.XpluginRequire(plugin)
      case s"-Xpluginsdir$path"       => PluginSpecific.Xpluginsdir(path)

      case _ => NotParsed(s)
    }

  def sanitize(initial: Seq[String]): Seq[String] = {
    val line   = initial.mkString(" ")
    val tokens = CommandLineParser.tokenize(line) // could throw an exception

    @tailrec
    def loop(args: Seq[String], res: Seq[String]): Seq[String] =
      args match {
        case Nil => res
        case first :: second :: tail if first.startsWith("-") && !second.startsWith("-") =>
          loop(tail, res :+ (s"$first $second"))

        case first :: tail => loop(tail, res :+ first)
      }
    loop(tokens, Nil)
  }

  sealed abstract class Renamed(val scala2Value: String, val scala3Value: String)
      extends Scala2cOption
      with Scala3cOption

  sealed abstract class Specific2(val scala2Value: String) extends Scala2cOption
  sealed abstract class Specific3(val scala3Value: String) extends Scala3cOption
  case class NotParsed(value: String) extends ScalacOption {
    lazy val valueForBuild: Seq[String] = value.split(" ").toSeq
  }

  sealed abstract class PluginSpecific(value: String) extends Scala2cOption with Scala3cOption {
    override val scala2Value: String = value
    override val scala3Value: String = value
  }

  sealed abstract class Shared(value: String) extends Scala2cOption with Scala3cOption {
    override val scala2Value: String = value
    override val scala3Value: String = value
  }

  object Shared {
    // standard settings
    case class JvmArg(flag: String)            extends Shared(s"-J$flag")
    case class Property(property: String)      extends Shared(s"-D$property")
    case class Bootclasspath(path: String)     extends Shared(s"-bootclasspath$path")
    case class Classpath(path: String)         extends Shared(s"-classpath$path")
    case class D(directoryOrJar: String)       extends Shared(s"-d$directoryOrJar")
    case object Deprecation                    extends Shared("-deprecation")
    case class Encoding(encoding: String)      extends Shared(s"-encoding$encoding")
    case class Extdirs(path: String)           extends Shared(s"-extdirs$path")
    case object Feature                        extends Shared("-feature")
    case object Help                           extends Shared("-help")
    case class Javabootclasspath(path: String) extends Shared(s"-javabootclasspath$path")
    case class Javaextdirs(path: String)       extends Shared(s"-javaextdirs$path")
    case class Language(features: String)      extends Shared(s"-language$features")
    case object Nowarn                         extends Shared("-nowarn")
    case object Print                          extends Shared("-print")
    case class Release(release: String)        extends Shared(s"-release$release")
    case class SourcePath(path: String)        extends Shared(s"-sourcepath$path")
    case object Unchecked                      extends Shared("-unchecked")
    case object Uniqid                         extends Shared("-uniqid")
    case object Usejavacp                      extends Shared("-usejavacp")
    case object Verbose                        extends Shared("-verbose")
    case object Version                        extends Shared("-version")
    // verbose settings
    case object Vhelp                 extends Shared("-V")
    case class Vprint(phases: String) extends Shared(s"-Vprint$phases")
    case object Vphases               extends Shared("-Vphases")
    case object Vprofile              extends Shared("-Vprofile")
    // warning settings
    case object Whelp                    extends Shared("-W")
    case object Werror                   extends Shared("-Werror")
    case object WvalueDiscard            extends Shared("-Wvalue-discard")
    case class Wunused(warnings: String) extends Shared(s"-Wunused$warnings")
    case class Wconf(patterns: String)   extends Shared(s"-Wconf$patterns")
    // advanced settings
    case object XHelp                           extends Shared("-X")
    case object Xmigration                      extends Shared("-Xmigration")
    case object XmixinForceForwarders           extends Shared("-Xmixin-force-forwarders")
    case object XnoForwarders                   extends Shared("-Xno-forwarders")
    case object Xprompt                         extends Shared("-Xprompt")
    case class XmacroSettings(settings: String) extends Shared(s"-Xmacro-settings$settings")
    case class XmainClass(main: String)         extends Shared(s"-Xmain-class$main")
    // Private settings
    case object YHelp                                 extends Shared("-Y")
    case class Ycheck(phases: String)                 extends Shared(s"-Ycheck$phases")
    case class YdumpClasses(dir: String)              extends Shared(s"-Ydump-classes$dir")
    case object YnoGenericSignatures                  extends Shared("-Yno-generic-signatures")
    case object YnoImports                            extends Shared("-Yno-imports")
    case class Yimports(imports: String)              extends Shared(s"-Yimports$imports")
    case object YnoPredef                             extends Shared("-Yno-predef")
    case class YprofileDestination(file: String)      extends Shared(s"-Yprofile-destination$file")
    case class YprofileEnabled(strategy: String)      extends Shared(s"-Yprofile-enabled$strategy")
    case class YresolveTermConflict(strategy: String) extends Shared(s"-Yresolve-term-conflict$strategy")
    case class Yskip(phases: String)                  extends Shared(s"-Yskip$phases")
    case class YstopAfter(phases: String)             extends Shared(s"-Ystop-after$phases")
    case class YstopBefore(phases: String)            extends Shared(s"-Ystop-before$phases")
  }

  object PluginSpecific {
    // plugin specific scalacOptions
    case class Plugin(plugin: String)         extends PluginSpecific(s"-P$plugin")
    case class Xplugin(paths: String)         extends PluginSpecific(s"-Xplugin$paths")
    case class XpluginDisable(plugin: String) extends PluginSpecific(s"-Xplugin-disable$plugin")
    case class XpluginRequire(plugin: String) extends PluginSpecific(s"-Xplugin-require$plugin")
    case class Xpluginsdir(path: String)      extends PluginSpecific(s"-Xpluginsdir$path")
    case object XpluginList                   extends PluginSpecific("-Xplugin-list")
  }

  object Renamed {
    case object Explaintypes            extends Renamed("-explaintypes", "-explain")
    case class Xsource(version: String) extends Renamed(s"-Xsource$version", s"-source$version")
    case class Target(target: String)
        extends Renamed(s"-target$target", s"-Xunchecked-java-output-version${Target.parseTarget(target)}")
    object Target {
      def parseTarget(in: String): String =
        in match {
          case s"${sep}jvm-1.$number" => sep + number
          case in                     => in
        }
    }
    // advanced settings
    case object Xcheckinit  extends Renamed("-Xcheckinit", "-Ysafe-init")
    case object Xverify     extends Renamed("-Xverify", "-Xverify-signatures")
    case object VprintTypes extends Renamed("-Vprint-types", "-Xprint-types")
    // Scala.js Settings
    case object ScalaJsStatic
        extends Renamed(
          "-P:scalajs:genStaticForwardersForNonTopLevelObjects",
          "-scalajs-genStaticForwardersForNonTopLevelObjects"
        )
    Some(Renamed.Explaintypes) // `-scalajs-genStaticForwardersForNonTopLevelObjects` |
    case object ScalaJsURI extends Renamed("-P:scalajs:mapSourceURI", "-scalajs-mapSourceURI")

    // Warning and verbose settings
    case object XlintDeprecation    extends Renamed("-Xlint:deprecation", "-deprecation")
    case object Vclasspath          extends Renamed("-Vclasspath", "-Ylog-classpath")
    case class Vlog(phases: String) extends Renamed(s"-Vlog$phases", s"-Ylog$phases")
    case object Vdebug              extends Renamed("-Vdebug", "-Ydebug")
    case object VprintPos           extends Renamed("-Vprint-pos", "-Yprint-pos")
    case object VdebugTypeError     extends Renamed("-Vdebug-type-error", "-Ydebug-type-error")
  }

  case class Scala2Specific(value: String) extends Specific2(value)

  case class Scala3Specific(value: String) extends Specific3(value)
  case object KindProjector                extends Specific3("-Ykind-projector")
  case object SemanticDB                   extends Specific3("-Xsemanticdb")
  case class CoverageOut(out: String)      extends Specific3(s"-coverage-out$out")
}
