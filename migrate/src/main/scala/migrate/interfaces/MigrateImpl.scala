package migrate.interfaces

import migrate.interfaces.Migrate
import migrate.interfaces.MigrateService

final class MigrateImpl extends Migrate {
  override def getService: MigrateService = new MigrateServiceImpl()
}
