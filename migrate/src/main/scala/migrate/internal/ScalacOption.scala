package migrate.internal

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
      case s"-bootclasspath $path"     => Shared.Bootclasspath(path)
      case s"-classpath $path"         => Shared.Classpath(path)
      case s"-d $directoryOrJar"       => Shared.D(directoryOrJar)
      case "-deprecation"              => Shared.Deprecation
      case s"-encoding $encoding"      => Shared.Encoding(encoding)
      case "-explaintypes"             => Renamed.Explaintypes
      case s"-extdirs $path"           => Shared.Extdirs(path)
      case "-feature"                  => Shared.Feature
      case "-help"                     => Shared.Help
      case s"-javabootclasspath $path" => Shared.Javabootclasspath(path)
      case s"-javaextdirs $path"       => Shared.Javaextdirs(path)
      case s"-language:$features"      => Shared.Language(features)
      case "-nowarn"                   => Shared.Nowarn
      case "-print"                    => Shared.Print
      case s"-release $release"        => Shared.Release(release)
      case s"-Xsource:$version"        => Renamed.Xsource(version)
      case s"-source:$version"         => Renamed.Xsource(version)
      case s"-sourcepath $path"        => Shared.SourcePath(path)
      case s"-target:$target"          => Renamed.Target(target)
      case "-unchecked"                => Shared.Unchecked
      case "-uniqid"                   => Shared.Uniqid
      case "-usejavacp"                => Shared.Usejavacp
      case "-verbose"                  => Shared.Verbose
      case "-version"                  => Shared.Version
      // advanced settings
      case "-X"                                => Shared.XHelp
      case "-Xcheckinit"                       => Renamed.Xcheckinit
      case "-Xmigration"                       => Shared.Xmigration
      case "-Xmixin-force-forwarders"          => Shared.XmixinForceForwarders
      case "-Xno-forwarders"                   => Shared.XnoForwarders
      case s"-Xprompt"                         => Shared.Xprompt
      case s"-Xverify" | "-Xverify-signatures" => Renamed.Xverify
      case s"-Vprint-types" | "-Xprint-types"  => Renamed.VprintTypes

      // Private settings
      case "-Y"                                     => Shared.YHelp
      case s"-Ycheck:$phases"                       => Shared.Ycheck(phases)
      case s"-Ydump-classes $dir"                   => Shared.YdumpClasses(dir)
      case "-Yno-generic-signatures"                => Shared.YnoGenericSignatures
      case "-Yno-imports"                           => Shared.YnoImports
      case "-Yno-predef"                            => Shared.YnoPredef
      case s"-Yprofile-destination $file"           => Shared.YprofileDestination(file)
      case s"-Yprofile-enabofile-enabled:$strategy" => Shared.YprofileEnabled(strategy)
      case s"-Yresolve-term-conflict:$strategy"     => Shared.YresolveTermConflict(strategy)
      case s"-Yskip:$phases"                        => Shared.Yskip(phases)
      case s"-Ystop-after:$phases"                  => Shared.YstopAfter(phases)
      case s"-Ystop-before:$phases"                 => Shared.YstopBefore(phases)

      // Scala.js Settings
      case "-P:scalajs:genStaticForwardersForNonTopLevelObjects" |
          "-scalajs-genStaticForwardersForNonTopLevelObjects" =>
        Renamed.ScalaJsStatic
      case "-P:scalajs:mapSourceURI" | "-scalajs-mapSourceURI" => Renamed.ScalaJsURI

      // Warning and verbose settings
      case "-Werror" | "-Xfatal-warnings" => Renamed.Werror
      case "-Xlint:deprecation"           => Renamed.XlintDeprecation
      case s"-Vprint:$phases"             => Renamed.Vprint(phases)
      case "-Vphases"                     => Renamed.Vphases
      case "-Vclasspath"                  => Renamed.Vclasspath
      case s"-Vlog:$phases"               => Renamed.Vlog(phases)
      case "-Vdebug"                      => Renamed.Vdebug
      case "-Vprint-pos"                  => Renamed.VprintPos

      // Specific to scala 2
      case _ if s.startsWith("-D")                                                      => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-J")                                                      => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-dependencyfile")                                         => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-g")                                                      => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-no-specialization")                                      => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-nobootcp")                                               => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-opt")                                                    => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-opt-inline-from")                                        => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-opt-warnings")                                           => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-optimize")                                               => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-rootdir")                                                => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-toolcp")                                                 => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-usemanifestc")                                           => Specific2.Scala2Specific(s)
      case "-W"                                                                         => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Wconf")                                                  => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Wdead-code") || s.startsWith("-Ywarn-dead-code")         => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Wextra-implicit")                                        => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Wmacros")                                                => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Wnumeric-widen") || s.startsWith("-Ywarn-numeric-widen") => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Woctal-literal")                                         => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Wunused") || s.startsWith("-Ywarn-unused") =>
        Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Wvalue-discard") || s.startsWith("-Ywarn-value-discard") =>
        Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xlint")          => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Wself-implicit") => Specific2.Scala2Specific(s)
      case "-V"                                 => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vbrowse")        => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vdebug-tasty")   => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vdoc")           => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vfree-terms")    => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vfree-types")    => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vhot-statistics") || s.startsWith("-Yhot-statistics") =>
        Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vide") || s.startsWith("-Yide-debug") => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vimplicit-conversions") || s.startsWith("-implicit-conversion") =>
        Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vimplicits") || s.startsWith("-Xlog-implicits") => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vinline") || s.startsWith("-Yopt-log-inline") =>
        Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vissue") || s.startsWith("-Yissue-debug")           => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vmacro") || s.startsWith("-Ymacro-debug-ver")       => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vmacro-lite") || s.startsWith("-Ymacro-debug-lite") => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vopt") || s.startsWith("-Yopt-trace") =>
        Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vpatmat") || s.startsWith("-Ypatmat-debug") => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vpos") || s.startsWith("-Ypos-debug")       => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vprint-args") || s.startsWith("--Xprint-args") =>
        Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vquasiquote") || s.startsWith("Yquasiquote-debug") => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vreflective-calls") || s.startsWith("-Xlog-reflectiv") =>
        Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vreify") || s.startsWith("-Yreify-debug")      => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vshow") || s.startsWith("-Yshow")              => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vshow-class") || s.startsWith("-Xshow-object") => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vshow-member-pos") || s.startsWith("-Yshow-member-pos") =>
        Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vshow-object") || s.startsWith("-Xshow-object")     => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vshow-symkinds") || s.startsWith("-Yshow-symkinds") => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vshow-symowners") || s.startsWith("-Yshow-symowners") =>
        Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vstatistics") || s.startsWith("-Ystatistics") => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vsymbols") || s.startsWith("-Yshow-syms")     => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Vtyper") || s.startsWith("-Ytyper-debug")     => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xdev")                                        => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xdisable-assertions")                         => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xelide-below")                                => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xexperimental")                               => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xfuture")                                     => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xgenerate-phase-graph")                       => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xjline")                                      => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xmacro-settings")                             => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xmain-class")                                 => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xmaxerrs")                                    => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xmaxwarns")                                   => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xno-patmat-analysis")                         => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xnojline")                                    => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xreporter")                                   => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xresident")                                   => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xscript")                                     => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xsource-reader")                              => Specific2.Scala2Specific(s)
      case _ if s.startsWith("-Xxml")                                        => Specific2.Scala2Specific(s)

      case "-Ytasty-reader"                    => Specific2.Scala2Specific(s)
      case "-Ybackend-parallelism"             => Specific2.Scala2Specific(s)
      case "-Ybackend-worker-queue"            => Specific2.Scala2Specific(s)
      case "-Ybreak-cycles"                    => Specific2.Scala2Specific(s)
      case "-Ycache-macro-class-loader"        => Specific2.Scala2Specific(s)
      case "-Ycache-plugin-class-loader"       => Specific2.Scala2Specific(s)
      case "-Ycompact-trees"                   => Specific2.Scala2Specific(s)
      case "-Ydelambdafy"                      => Specific2.Scala2Specific(s)
      case "-Ygen-asmp"                        => Specific2.Scala2Specific(s)
      case "-Yimports"                         => Specific2.Scala2Specific(s)
      case "-Yjar-compression-level"           => Specific2.Scala2Specific(s)
      case "-YjarFactory"                      => Specific2.Scala2Specific(s)
      case "-Ymacro-annotations"               => Specific2.Scala2Specific(s)
      case "-Ymacro-classpath"                 => Specific2.Scala2Specific(s)
      case "-Ymacro-expand"                    => Specific2.Scala2Specific(s)
      case "-Ymacro-global-fresh-names"        => Specific2.Scala2Specific(s)
      case "-Yno-completion"                   => Specific2.Scala2Specific(s)
      case "-Yno-flat-classpath-cache"         => Specific2.Scala2Specific(s)
      case "-Yopt-inline-heuristics"           => Specific2.Scala2Specific(s)
      case "-Ypatmat-exhaust-depth"            => Specific2.Scala2Specific(s)
      case "-Ypresentation-any-thread"         => Specific2.Scala2Specific(s)
      case "-Ypresentation-debug"              => Specific2.Scala2Specific(s)
      case "-Ypresentation-delay"              => Specific2.Scala2Specific(s)
      case "-Ypresentation-locate-source-file" => Specific2.Scala2Specific(s)
      case "-Ypresentation-log"                => Specific2.Scala2Specific(s)
      case "-Ypresentation-strict"             => Specific2.Scala2Specific(s)
      case "-Ypresentation-verbose"            => Specific2.Scala2Specific(s)
      case "-Yprint-trees"                     => Specific2.Scala2Specific(s)
      case "-Yprofile-trace"                   => Specific2.Scala2Specific(s)
      case "-Yrangepos"                        => Specific2.Scala2Specific(s)
      case "-Yrecursion"                       => Specific2.Scala2Specific(s)
      case "-Yreify-copypaste"                 => Specific2.Scala2Specific(s)
      case "-Yrepl-class-based"                => Specific2.Scala2Specific(s)
      case "-Yrepl-outdir"                     => Specific2.Scala2Specific(s)
      case "-Yrepl-use-magic-imports"          => Specific2.Scala2Specific(s)
      case "-Yscriptrunner"                    => Specific2.Scala2Specific(s)
      case "-Yvalidate-pos"                    => Specific2.Scala2Specific(s)

      // specific 3
      case _ if s.startsWith("-color")                      => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-doc-snapshot")               => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-explain")                    => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-from-tasty")                 => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-indent")                     => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-new-syntax")                 => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-noindent")                   => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-old-syntax")                 => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-pagewidth")                  => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-print-lines")                => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-print-tasty")                => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-project")                    => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-project-logo")               => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-project-url")                => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-project-version")            => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-rewrite")                    => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-semanticdb-target")          => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-siteroot")                   => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-sourceroot")                 => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Xignore-scala2-macros")      => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ximport-suggestion-timeout") => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Xmax-inlined-trees")         => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Xmax-inlines")               => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Xprint-diff")                => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Xprint-diff-del")            => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Xprint-inline")              => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Xprint-suspension")          => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Xrepl-disable-display")      => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Xwiki-syntax")               => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ycheck-all-patmat")          => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ycheck-mods")                => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ycheck-reentrant")           => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ycook-comments")             => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-error")               => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-flags")               => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-missing-refs")        => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-names")               => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-pos")                 => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-trace")               => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-tree-with-id")        => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ydebug-type-error")          => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ydetailed-stats")            => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-YdisableFlatCpCaching")      => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ydrop-comments")             => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ydump-sbt-inc")              => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yerased-terms")              => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yexplain-lowlevel")          => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yexplicit-nulls")            => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yforce-sbt-phases")          => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yfrom-tasty-ignore-list")    => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yindent-colons")             => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yinstrument")                => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yinstrument-defs")           => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yno-decode-stacktraces")     => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yno-deep-subtypes")          => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yno-double-bindings")        => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yno-kind-polymorphism")      => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yno-patmat-opt")             => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yplain-printer")             => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yprint-debug")               => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yprint-debug-owners")        => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yprint-pos")                 => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yprint-pos-syms")            => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yprint-syms")                => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yrequire-targetName")        => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yretain-trees")              => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yscala2-unpickler")          => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yshow-print-errors")         => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yshow-suppressed-errors")    => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yshow-tree-ids")             => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yshow-var-bounds")           => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Ytest-pickler")              => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Yunsound-match-types")       => Specific3.Scala3Specific(s)
      case _ if s.startsWith("-Xsemanticdb")                => Specific3.SemanticDB
      case _ if s.startsWith("-Ykind-projector")            => Specific3.KindProjector

      // plugin specific scalacOption
      case s"-P:$plugin"               => PluginSpecific.Plugin(plugin)
      case s"-Xplugin:$paths"          => PluginSpecific.Xplugin(paths)
      case s"-Xplugin-disable:$plugin" => PluginSpecific.XpluginDisable(plugin)
      case s"-Xplugin-list"            => PluginSpecific.XpluginList
      case s"-Xplugin-require:$plugin" => PluginSpecific.XpluginRequire(plugin)
      case s"-Xpluginsdir $path"       => PluginSpecific.Xpluginsdir(path)

      case _ => NotParsed(s)
    }

  def sanitizeScalacOption(initial: Seq[String]): Seq[String] = {
    val line   = initial.mkString(" ")
    val tokens = CommandLineParser.tokenize(line) // could throw an exception

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
    case class Bootclasspath(path: String)     extends Shared(s"-bootclasspath $path")
    case class Classpath(path: String)         extends Shared(s"-classpath $path")
    case class D(directoryOrJar: String)       extends Shared(s"-d $directoryOrJar")
    case object Deprecation                    extends Shared("-deprecation")
    case class Encoding(encoding: String)      extends Shared(s"-encoding $encoding")
    case class Extdirs(path: String)           extends Shared(s"-extdirs $path")
    case object Feature                        extends Shared("-feature")
    case object Help                           extends Shared("-help")
    case class Javabootclasspath(path: String) extends Shared(s"-javabootclasspath $path")
    case class Javaextdirs(path: String)       extends Shared(s"-javaextdirs $path")
    case class Language(features: String)      extends Shared(s"-language:$features")
    case object Nowarn                         extends Shared("-nowarn")
    case object Print                          extends Shared("-print")
    case class Release(release: String)        extends Shared(s"-release $release")
    case class SourcePath(path: String)        extends Shared(s"-sourcepath $path")
    case object Unchecked                      extends Shared("-unchecked")
    case object Uniqid                         extends Shared("-uniqid")
    case object Usejavacp                      extends Shared("-usejavacp")
    case object Verbose                        extends Shared("-verbose")
    case object Version                        extends Shared("-version")
    // advanced settings
    case object XHelp                 extends Shared("-X")
    case object Xmigration            extends Shared("-Xmigration")
    case object XmixinForceForwarders extends Shared("-Xmixin-force-forwarders")
    case object XnoForwarders         extends Shared("-Xno-forwarders")
    case object Xprompt               extends Shared("-Xprompt")
    // Private settings
    case object YHelp                                 extends Shared("-Y")
    case class Ycheck(phases: String)                 extends Shared(s"-Ycheck:$phases")
    case class YdumpClasses(dir: String)              extends Shared(s"-Ydump-classes $dir")
    case object YnoGenericSignatures                  extends Shared("-Yno-generic-signatures")
    case object YnoImports                            extends Shared("-Yno-imports")
    case object YnoPredef                             extends Shared("-Yno-predef")
    case class YprofileDestination(file: String)      extends Shared(s"-Yprofile-destination $file")
    case class YprofileEnabled(strategy: String)      extends Shared(s"-Yprofile-enabofile-enabled:$strategy")
    case class YresolveTermConflict(strategy: String) extends Shared(s"-Yresolve-term-conflict:$strategy")
    case class Yskip(phases: String)                  extends Shared(s"-Yskip:$phases")
    case class YstopAfter(phases: String)             extends Shared(s"-Ystop-after:$phases")
    case class YstopBefore(phases: String)            extends Shared(s"-Ystop-before:$phases")

  }

  object PluginSpecific {
    // plugin specific scalacOptions
    case class Plugin(plugin: String)         extends PluginSpecific(s"-P:$plugin")
    case class Xplugin(paths: String)         extends PluginSpecific(s"-Xplugin:$paths")
    case class XpluginDisable(plugin: String) extends PluginSpecific(s"-Xplugin-disable:$plugin")
    case class XpluginRequire(plugin: String) extends PluginSpecific(s"-Xplugin-require:$plugin")
    case class Xpluginsdir(path: String)      extends PluginSpecific(s"-Xpluginsdir $path")
    case object XpluginList                   extends PluginSpecific("-Xplugin-list")
  }

  object Renamed {
    case object Explaintypes            extends Renamed("-explaintypes", "-explain-types")
    case class Xsource(version: String) extends Renamed(s"-Xsource:$version", s"-source:$version")
    case class Target(target: String)   extends Renamed(s"-target:$target", s"-Xtarget:${Target.parseTarget(target)}")
    object Target {
      def parseTarget(in: String): String =
        in match {
          case s"jvm-1.$number" => number
          case in               => in
        }
    }
    // advanced settings
    case object Xcheckinit  extends Renamed("-Xcheckinit", "-Ycheck-init")
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
    case object Werror                extends Renamed("-Werror", "-Xfatal-warnings")
    case object XlintDeprecation      extends Renamed("-Xlint:deprecation", "-deprecation")
    case class Vprint(phases: String) extends Renamed(s"-Vprint:phases", s"-Xprint:phases")
    case object Vphases               extends Renamed("-Vphases", "-Xshow-phases")
    case object Vclasspath            extends Renamed("-Vclasspath", "-Ylog-classpath")
    case class Vlog(phases: String)   extends Renamed(s"-Vlog:$phases", s"-Ylog:$phases")
    case object Vdebug                extends Renamed("-Vdebug", "-Ydebug")
    case object VprintPos             extends Renamed("-Vprint-pos", "-Yprint-pos")

  }

  object Specific2 {
    case class Scala2Specific(value: String) extends Specific2(value)
  }

  object Specific3 {
    case class Scala3Specific(value: String) extends Specific3(value)
    case object KindProjector                extends Specific3("-Ykind-projector")
    case object SemanticDB                   extends Specific3("-Xsemanticdb")
  }
}
