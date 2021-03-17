package migrate.interfaces;

import java.util.List;
import java.util.Map;

public interface MigratedLibs {
    Lib[] getNotMigrated();
    Map<Lib, List<Lib>> getLibsToUpdate();
    Lib[] getValidLibs();
    Map<Lib, String> getMigratedCompilerPlugins();
}