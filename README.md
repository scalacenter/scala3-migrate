# scala3-migrate
This project aims to make the migration to Scala 3 easier:

- migrate-libs: It will help you update the list of your `libraryDependencies`
- migrate-scalacOptions: It will help you update your `scalacOptions`
- migrate-prepare: Fix no more supported syntax in Scala 3
- migrate: Add the minimum set of types required explicitly to make Scala 3 compiling a project without changing its meaning

# Requirements 
- sbt 1.3 or higher
- java 8 or 11
- scala 2.13, preferred 2.13.5

# Installation
```
addSbtPlugin("ch.epfl.scala" % "sbt-scala3-migrate" % "0.2.0")
// sbt-dotty is not needed anymore since sbt 1.5.0-M1
addSbtPlugin("ch.epfl.lamp"  % "sbt-dotty"          % "0.5.3")
```

# Porting the build

The first step of the migration is to choose a single module that is going to be ported first.
Make sure it's not an aggregate of projects. 
We will proceed with its migration then we will start again with another module.

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
[info]
[info] Starting to migrate libDependencies for main
[info]
[info] The following compiler plugins are not supported in scala 3.0.0-RC1
[info] You need to find alternatives. Please check the migration guide for more information.
[info]
[info] com.olegpy:better-monadic-for:0.3.1:plugin->default(compile)
[info]
[info] The following list of libs cannot be migrated as they contain Macros and are not yet
[info] published for 3.0.0-RC1
[info]
[info] com.softwaremill.scalamacrodebug:macros:0.4.1:test
[info]
[info]
[info] The following compiler plugins are not supported in scala 3.0.0-RC1
[info] but there is an equivalent scalacOption that can replace it.
[info] Add these scalacOptions to your ScalacOptions:
[info]
[info] "org.typelevel:kind-projector:0.11.0:plugin->default(compile)" -> -Ykind-projector
[info]
[info]
[info]
[info] You can update your libs with the following versions:
[info]
[info] "org.typelevel:cats-core:2.2.0" -> "org.typelevel" %% "cats-core" % "2.4.2"
[info] "ch.epfl.scala:scalafix-rules:0.9.26:test" -> "ch.epfl.scala" % "scalafix-rules_2.13" % "0.9.26" % "test"
[info] "ch.epfl.scala:scalafix-interfaces:0.9.26" -> "ch.epfl.scala" % "scalafix-interfaces" % "0.9.26"
[info]
```

In this case you need to find alternative to `better-monadic-for` and make the project compile without the plugin.
For `com.softwaremill.scalamacrodebug:macros` which is a macro lib, it won't be possible to migrate until 
it's published for `3.0.0-RC1`.

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
[info] Starting to migrate the scalacOptions for main
[warn] Some scalacOptions are set by sbt plugins and don't need to be modified, removed or added.
[warn] The sbt plugin should adapt its own scalacOptions for Scala 3
[info]
[info] X         : The following scalacOption is specific to Scala 2 and doesn't have an equivalent in Scala 3
[info] Renamed   : The following scalacOption has been renamed in Scala3
[info] ✔         : The following scalacOption is a valid Scala 3 option
[info]
[info] "-Wunused" -> X
[info] "-Yrangepos" -> X
[info] "-explaintypes" -> "-explain-types"
[info]
[info]
[info]
[info] The following scalacOption are specific to compiler plugins, usually added through `compilerPlugin` or `addCompilerPlugin`.
[info] In the previous step `migrate-libs`, you should have removed/fixed compiler plugins and for the remaining plugins and settings, they can be kept as they are.
[info]
[info] "-Xplugin:/Users/meriamlachkar/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/typelevel/kind-projector_2.13.3/0.11.0/kind-projector_2.13.3-0.11.0.jar" -> ✔
[info] "-Xplugin:/Users/meriamlachkar/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scalameta/semanticdb-scalac_2.13.3/4.4.0/semanticdb-scalac_2.13.3-4.4.0.jar" -> ✔
[info] "-P:semanticdb:synthetics:on" -> ✔
[info] "-P:semanticdb:targetroot:/Users/meriamlachkar/perso/plugin-test/target/scala-2.13/meta" -> ✔
[info] "-P:semanticdb:sourceroot:/Users/meriamlachkar/perso/plugin-test" -> ✔
[info] "-P:semanticdb:failures:warning" -> ✔
[info]
```

So following the output, the build will look like:
```
    scalacOptions ++= (if (scalaVersion.value.startsWith("3.0")) Seq("-explain-types", "-Wunused", "-Ykind-projector")
                       else Seq("-explaintypes")),
```


# Migrating the code
## Fix syntax incompatibilities 
First reload the build to take into account the new libs and scalacOptions.
```
> reload
```

The command `migrate-prepare` fix number of incompatibilities by applying the following scalafix rules:
- ProcedureSyntax
- fix.scala213.ConstructorProcedureSyntax
- fix.scala213.ExplicitNullaryEtaExpansion
- fix.scala213.ParensAroundLambda
- fix.scala213.ExplicitNonNullaryApply
- fix.scala213.Any2StringAdd

```
> migrate-prepare main
[success] Total time: 0 s, completed 12 Mar 2021, 20:55:51
[info] We are going to fix some syntax incompatibilities
[INFO ] migrate.ScalaMigrat.previewPrepareMigration:79 - Successfully run fixSyntaxForScala3  in 7280 milliseconds
[INFO ] migrate.utils.ScalafixService.fixInPlace:40 - Syntax fixed for Incompat13.scala)
[INFO ] migrate.utils.ScalafixService.fixInPlace:40 - Syntax fixed for Cats6.scala)
[INFO ] migrate.utils.ScalafixService.fixInPlace:40 - Syntax fixed for Cats5.scala)
[info]
[info]
[info] We fixed the syntax of this main to be compatible with 3.0.0-RC1
[info] You can now commit the change!
[info] You can also execute the next command to try to migrate to 3.0.0-RC1
[info]
[info] migrate main
[info]
```
After running this command, you can already commit those changes. Your module is still compiling in Scala 2 
and is one step further to compile in scala 3. 

## Add inferred types and implicits to make the project compile in scala 3
Scala 3 uses a new type inference algorithm, therefore the Scala 3.0 compiler can infer a different type 
than the one inferred by the Scala 2.13.
This command goal is to find the necessary types to add in order to make you code compiles.
``` 
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
[info] main project has successfully been migrated to scala 3.0.0-RC1
[info] You can now commit the change!
[info] You can also execute the compile command:
[info]
[info] main / compile
```

# Acknowledgments
<img src="https://scala.epfl.ch/resources/img/scala-center-swirl.png" width="40px" /> This tool is developed by [Scala Center](https://scala.epfl.ch)
