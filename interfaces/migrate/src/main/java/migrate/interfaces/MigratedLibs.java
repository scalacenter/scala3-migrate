package migrate.interfaces;

import java.util.List;
import java.util.Map;

public interface MigratedLibs {
    MigratedLib[] getUncompatibleWithScala3();
    Map<Lib, MigratedLib> getLibsToUpdate();
    MigratedLib[] getValidLibs();
}