#!/usr/bin/env bash
set -eux

version=$1

cs resolve \
  ch.epfl.scala:scala3-migrate-compiler-interface:$version  \
  ch.epfl.scala:scala3-migrate-interface:$version  \
  ch.epfl.scala:scala3-migrate-rules_2.13:$version  \
  ch.epfl.scala:scala3-migrate-core_2.13:$version  \
  -r sonatype:publics

cs resolve \
    --sbt-version 1.0 \
    --sbt-plugin "ch.epfl.scala:sbt-scala3-migrate:$version"
