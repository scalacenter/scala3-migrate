package migrate.interfaces;

import java.util.List;
import java.util.Map;

public interface MigratedLibs {
    Lib[] getNotMigrated();
    Map<Lib, List<Lib>> getMigrated();
    Map<Lib, String> getMigratedCompilerPlugins();
}