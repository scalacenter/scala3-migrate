# Contributing

## Scalafix

We use scalafix to apply some rules that are configured in .scalafix.conf. 
Make sure to run `sbt scalafixAll` to apply those rules before opening a pull request.

## Scalafmt

Be sure to run scalafmt (available in `bin/` folder) to ensure code formatting. 
`bin/scalafmt --diff` formats only the files that have changed from the master branch.
