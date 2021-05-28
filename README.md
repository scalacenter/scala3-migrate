scala3-migrate
[![Latest version](https://index.scala-lang.org/scalacenter/scala3-migrate/scala3-migrate/latest.svg)](https://index.scala-lang.org/scalacenter/scala3-migrate/scala3-migrate)
[![Build status](https://github.com/scalacenter/scala3-migrate/workflows/CI/badge.svg)](https://github.com/scalacenter/scala3-migrate/actions?query=workflow)
========
# User documentation
For detailed documentation, refer to `scala3-migrate` [section in the migration guide](https://scalacenter.github.io/scala-3-migration-guide/docs/tooling/scala-3-migrate-plugin.html)

# Usage

**scala3-migrate** has been designed to make the migration to scala 3 easier.

It proposes an incremental approach that can be described as follows:
- Migrating the library dependencies: using Coursier, it checks,
  for every library dependency, if there are versions available for Scala 3.
- Migrating the Scala compiler options (`scalacOptions`): some compiler options of Scala 2 have been removed
  in Scala 3, others have been renamed, and some remain the same.
  This step helps you find how to evolve the compiler options of your project.
- Migrating the syntax: this step relies on Scalafix and on existing rules to fix deprecated
  syntax that no longer compiles in Scala 3
- Migrating the code: as explained in [the section of type-inference](incompatibilities/table.md#type-inference),
  Scala 3 has a new type inference algorithm that may infer a different type than the one inferred
  by the Scala 2 compiler. This last step tries to find the minimum set of
  types to explicitly annotate in order to make the Scala 3 compiler work on a project without changing its meaning.

# Requirements 
- sbt 1.4 or higher
- java 8 or 11
- scala 2.13, preferred 2.13.5

# Installation
```
// project/plugins.sbt
addSbtPlugin("ch.epfl.scala" % "sbt-scala3-migrate" % "0.4.3")
// sbt-dotty is not needed anymore since sbt 1.5.0-M1
addSbtPlugin("ch.epfl.lamp"  % "sbt-dotty"          % "0.5.3")
```

# Porting the build

The first step of the migration is to choose a single module that is going to be ported first.
We will proceed with its migration then we will start again with another module.

> If the chosen module is an aggregate project, only its own sources will be migrated, not the sources of its subprojects.
> Each subproject needs to be ported separately, one at a time.

We will port this simple build definition
```
lazy val main = project
  .in(file("."))
  .settings(
    name := "main",
    scalaVersion := V.scala213,
    semanticdbEnabled := true,
    scalacOptions ++= Seq("-explaintypes", "-Wunused"),
    libraryDependencies += compilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
    libraryDependencies += compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core"           % "2.2.0",
      "ch.epfl.scala"  % "scalafix-interfaces" % "0.9.26",
      "com.softwaremill.scalamacrodebug" %% "macros" % "0.4.1" % Test,
      "ch.epfl.scala" %% "scalafix-rules"      % "0.9.26" % Test
    )
  )
```

## Migrating `libraryDependencies`

```
> migrate-libs main
[info] Starting to migrate libDependencies for main
[info]
[info] X             : Cannot be updated to scala 3
[info] Valid         : Already a valid version for Scala 3
[info] To be updated : Need to be updated to the following version
[info]
[info] com.softwaremill.scalamacrodebug:macros:0.4.1:test           -> X : Contains Macros and is not yet published for 3.0.0
[info] com.olegpy:better-monadic-for:0.3.1:plugin->default(compile) -> X : Scala 2 compiler plugins are not supported in scala 3.0.0. You need to find an alternative
[info] ch.epfl.scala:scalafix-interfaces:0.9.26                     -> Valid
[info] org.typelevel:cats-core:2.2.0                                -> "org.typelevel" %% "cats-core" % "2.4.2"
[info] ch.epfl.scala:scalafix-rules:0.9.26:test                     -> "ch.epfl.scala" % "scalafix-rules_2.13" % "0.9.26" % "test"
[info] org.typelevel:kind-projector:0.11.0:plugin->default(compile) -> -Ykind-projector : This compiler plugin has a scalacOption equivalent. Add it to your scalacOptions
```

In this case you need to find alternative to `better-monadic-for` and make the project compile without the plugin.
For `com.softwaremill.scalamacrodebug:macros` which is a macro lib, it won't be possible to migrate until 
it's published for `3.0.0`.

The ported build would look like: (if we consider possible to remove `com.softwaremill.scalamacrodebug:macros`)
```
lazy val main = project
  .in(file("."))
  .settings(
    name := "main",
    scalaVersion := V.scala213,
    semanticdbEnabled := true,
    scalacOptions ++= (if (scalaVersion.value.startsWith("3.0")) Seq("-explaintypes", "-Wunused", "-Ykind-projector")
                       else Seq("-explaintypes", "-Wunused")),
    libraryDependencies ++= (
      if (scalaVersion.value.startsWith("3.0")) Seq()
      else
        Seq(compilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full))
    ),
    libraryDependencies ++= Seq(
      "org.typelevel"                    %% "cats-core"           % "2.4.2",
      "ch.epfl.scala"                     % "scalafix-interfaces" % "0.9.26",
      "ch.epfl.scala"                    % "scalafix-rules_2.13" % "0.9.26" % Test
    )
  )
```

## Migrating scalacOptions

```
> migrate-scalacOptions main
[info]
[info] Starting to migrate the scalacOptions for main
[warn] Some scalacOptions are set by sbt plugins and don't need to be modified, removed or added.
[warn] The sbt plugin should adapt its own scalacOptions for Scala 3
[info]
[info] X       : The following scalacOption is specific to Scala 2 and doesn't have an equivalent in Scala 3
[info] Renamed : The following scalacOption has been renamed in Scala3
[info] Valid   : The following scalacOption is a valid Scala 3 option
[info]
[info] -Wunused      -> X
[info] -Yrangepos    -> X
[info] -explaintypes -> -explain-types
[info]
[info] The following scalacOption are specific to compiler plugins, usually added through `compilerPlugin` or `addCompilerPlugin`.
[info] In the previous step `migrate-libs`, you should have removed/fixed compiler plugins and for the remaining plugins and settings, they can be kept as they are.
[info]
[info] -Xplugin:/Users/meriamlachkar/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/typelevel/kind-projector_2.13.3/0.11.0/kind-projector_2.13.3-0.11.0.jar     -> Valid
[info] -Xplugin:/Users/meriamlachkar/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/olegpy/better-monadic-for_2.13/0.3.1/better-monadic-for_2.13-0.3.1.jar      -> Valid
[info] -Xplugin:/Users/meriamlachkar/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scalameta/semanticdb-scalac_2.13.3/4.4.0/semanticdb-scalac_2.13.3-4.4.0.jar -> Valid
[info] -P:semanticdb:synthetics:on                                                                                                                                           -> Valid
[info] -P:semanticdb:targetroot:/Users/meriamlachkar/perso/plugin-test/target/scala-2.13/meta                                                                                -> Valid
[info] -P:semanticdb:sourceroot:/Users/meriamlachkar/perso/plugin-test                                                                                                       -> Valid
[info] -P:semanticdb:failures:warning                                                                                                                                        -> Valid
[success]                                                                                                                                      -> Valid
```

So following the output, the build will look like:
```
    scalacOptions ++= (if (scalaVersion.value.startsWith("3")) Seq("-explain-types", "-Ykind-projector")
                       else Seq("-explaintypes",  "-Wunused")),
```


# Migrating the code
## Fix syntax incompatibilities 
First reload the build to take into account the new libs and scalacOptions.
```
> reload
```

The command `migrate-syntax` fix number of incompatibilities by applying the following Scalafix rules:
- ProcedureSyntax
- fix.scala213.ConstructorProcedureSyntax
- fix.scala213.ExplicitNullaryEtaExpansion
- fix.scala213.ParensAroundLambda
- fix.scala213.ExplicitNonNullaryApply
- fix.scala213.Any2StringAdd

```
> migrate-syntax main
[success] Total time: 0 s, completed 12 Mar 2021, 20:55:51
[info] We are going to fix some syntax incompatibilities
[INFO ] migrate.ScalaMigrat.previewSyntaxMigration:79 - Successfully run fixSyntaxForScala3  in 7280 milliseconds
[INFO ] migrate.utils.ScalafixService.fixInPlace:40 - Syntax fixed for Incompat13.scala)
[INFO ] migrate.utils.ScalafixService.fixInPlace:40 - Syntax fixed for Cats6.scala)
[INFO ] migrate.utils.ScalafixService.fixInPlace:40 - Syntax fixed for Cats5.scala)
[info]
[info]
[info] We fixed the syntax of this main to be compatible with 3.0.0
[info] You can now commit the change!
[info] You can also execute the next command to try to migrate to 3.0.0
[info]
[info] migrate main
[info]
```
After running this command, you can already commit those changes. Your module is still compiling in Scala 2 
and is one step further to compile in Scala 3. 

## Add inferred types and implicits to make the project compile in Scala 3
Scala 3 uses a new type inference algorithm, therefore the Scala 3.0 compiler can infer a different type 
than the one inferred by the Scala 2.13.
This command goal is to find the necessary types to add in order to make you code compiles.
``` 
> migrate main
[info] We are going to migrate your project main to scala 3
[INFO ] migrate.ScalaMigrat.buildMigrationFiles:140 - Found 20 patch candidate(s) in 7 file(s)after 1254 milliseconds
[INFO ] migrate.ScalaMigrat.compileInScala3:115 - Successfully compiled with scala 3 in 5997 milliseconds
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 5 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 2 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 1 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.migrate:24 - Found 1 required patch(es) in Incompat7.scala after 4385 milliseconds ms
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 2 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 1 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.migrate:24 - Found 1 required patch(es) in Incompat5.scala after 2243 milliseconds ms
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 2 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 1 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.migrate:24 - Found 1 required patch(es) in Incompat3.scala after 302 milliseconds ms
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 1 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.migrate:24 - Found 1 required patch(es) in ReflectiveCall.scala after 773 milliseconds ms
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 4 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 2 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 1 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.migrate:24 - Found 1 required patch(es) in Incompat9.scala after 1951 milliseconds ms
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 3 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 1 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.migrate:24 - Found 1 required patch(es) in PrivateLocalImplicitWithoutType.scala after 313 milliseconds ms
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 3 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.loopUntilNoCandidates:41 - 1 remaining candidate(s)
[INFO ] migrate.internal.FileMigration.migrate:24 - Found 1 required patch(es) in Incompat4.scala after 1225 milliseconds ms
[INFO ] migrate.ScalaMigrat.x$11:67 - Incompat7.scala has been successfully migrated
[INFO ] migrate.ScalaMigrat.x$11:67 - Incompat5.scala has been successfully migrated
[INFO ] migrate.ScalaMigrat.x$11:67 - Incompat9.scala has been successfully migrated
[INFO ] migrate.ScalaMigrat.x$11:67 - PrivateLocalImplicitWithoutType.scala has been successfully migrated
[INFO ] migrate.ScalaMigrat.x$11:67 - Incompat4.scala has been successfully migrated
[INFO ] migrate.ScalaMigrat.x$11:67 - ReflectiveCall.scala has been successfully migrated
[INFO ] migrate.ScalaMigrat.x$11:67 - Incompat3.scala has been successfully migrated
[info]
[info]
[info] main project has successfully been migrated to scala 3.0.0
[info] You can now commit the change!
[info] You can also execute the compile command:
[info]
[info] main / compile
```

# What to do next ?
You can start again with another module `MODULE2`. If `MODULE2` depends on the last module migrated, you need
either to add `-Ytasty-reader` to `MODULE2` scalacOptions, or `reload` or `set MODULE-MIGRATED/scalaVersion := "2.13.5"`

Once you're done, you can remove `scala3-migrate` from your plugins.

# Contributions and feedbacks are welcome
The tool is still under development, and **we would love to hear from you.**
Every feedback will help us build a better tool: typos, clearer log messages, better documentation, bug reports, ideas of features,
so please open [GitHub issues](https://github.com/scalacenter/scala3-migrate) or contact us on [gitter](https://gitter.im/scala/center).

# Acknowledgments
<img src="https://scala.epfl.ch/resources/img/scala-center-swirl.png" width="40px" /> This tool is developed by [Scala Center](https://scala.epfl.ch)
