# Contributing

## Scalafix

We use scalafix to apply some rules that are configured in .scalafix.conf. 
Make sure to run `sbt scalafixAll` to apply those rules before opening a pull request.

## Scalafmt

Be sure to run scalafmt (available in `bin/` folder) to ensure code formatting. 
`bin/scalafmt --diff` formats only the files that have changed from the main branch.

## Release

First, kickstart a CI release to Sonatype by pushing a git tag that correspond to the desired commit
```
git fetch && git log origin/main --pretty=oneline # choose the commit hash you want to tag
COMMIT_HASH=70e06a8755dc3ca07e270fb1b1982cb45a863024 # change this variable
VERSION=0.1.0 # change this variable
git tag -af "v$VERSION" $COMMIT_HASH -m "v$VERSION" && git push -f origin v$VERSION
```
While the CI is running, update the release notes at https://github.com/scalacenter/scala3-migrate/releases

When scala3-migrate has completed the release, edit the release draft 
in the GitHub web UI to point to the tag that you pushed and then click on "Publish release".

