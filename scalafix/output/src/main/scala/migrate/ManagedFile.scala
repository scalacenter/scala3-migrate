package migrate

import buildinfo.BuildInfo

object ManagedFile {
  val string: String = BuildInfo.name
}