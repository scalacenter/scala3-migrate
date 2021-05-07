#!/usr/bin/env bash
set -eux

version=$1

cs resolve \
  ch.epfl.scala:migrate-compiler-interfaces:$version  \
  ch.epfl.scala:migrate-core-interfaces:$version  \
  ch.epfl.scala:migrate-rules_2.13:$version  \
  ch.epfl.scala:migrate-core_2.13:$version  \
  -r sonatype:publics

cs resolve \
    --sbt-version 1.0 \
    --sbt-plugin "ch.epfl.scala:sbt-scala3-migrate:$version"
