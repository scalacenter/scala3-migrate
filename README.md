scala3-migrate
[![Latest version](https://index.scala-lang.org/scalacenter/scala3-migrate/scala3-migrate/latest.svg)](https://index.scala-lang.org/scalacenter/scala3-migrate/scala3-migrate)
[![Build status](https://github.com/scalacenter/scala3-migrate/workflows/CI/badge.svg)](https://github.com/scalacenter/scala3-migrate/actions?query=workflow)
========

# Scala 3 migration plugin for sbt

## User documentation

The complete documentation of `sbt-scala3-migrate` can be found in [docs.scala-lang.org](https://docs.scala-lang.org/scala3/guides/migration/scala3-migrate.html).

## Usage

### Requirements 
- sbt 1.5 or higher
- java 8 or 11
- scala 2.13, preferred 2.13.11

### Installation
```
// project/plugins.sbt
addSbtPlugin("ch.epfl.scala" % "sbt-scala3-migrate" % "0.6.1")
```

### Porting the build

To port a build to Scala 3, run the following commands in order, in each project of the build: 
- `migrateDependencies <project>` helps you update the list of `libraryDependencies`
- `migrateScalacOptions <project>` helps you update the list of `scalacOptions`
- `migrateSyntax <project>` fixes a number of syntax incompatibilities between Scala 2.13 and Scala 3 
- `migrateTypes <project>` tries to make your code compile with Scala 3 by inferring a few types and resolving a few implicits.

## Contributions and feedbacks are welcome
The tool is still under development, and **we would love to hear from you.**
Every feedback will help us build a better tool: typos, clearer log messages, better documentation, bug reports, ideas of features,
so please open [GitHub issues](https://github.com/scalacenter/scala3-migrate).

## Acknowledgments
<img src="https://scala.epfl.ch/resources/img/scala-center-swirl.png" width="40px" /> This tool is developed by the [Scala Center](https://scala.epfl.ch)
