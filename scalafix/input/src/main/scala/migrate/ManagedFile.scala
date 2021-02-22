/*
rule = [MigrationRule]
*/
package migrate

import buildinfo.BuildInfo

object ManagedFile {
  val string = BuildInfo.name
}
