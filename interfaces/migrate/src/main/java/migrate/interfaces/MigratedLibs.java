package migrate.interfaces;

import java.util.List;
import java.util.Map;

public class MigratedLibs {
    private MigratedLib[] validLibraries;
    private MigratedLib[] updatedVersions;
    private MigratedLib[] crossCompatibleLibraries;
    private MigratedLib[] integratedPlugins;
    private MigratedLib[] unclassifiedLibraries;
    private MigratedLib[] incompatibleLibraries;

    public MigratedLibs(
        MigratedLib[] validLibraries,
        MigratedLib[] updatedVersions,
        MigratedLib[] crossCompatibleLibraries,
        MigratedLib[] integratedPlugins,
        MigratedLib[] unclassifiedLibraries,
        MigratedLib[] incompatibleLibraries
    ) {
        this.validLibraries = validLibraries;
        this.updatedVersions = updatedVersions;
        this.crossCompatibleLibraries = crossCompatibleLibraries;
        this.integratedPlugins = integratedPlugins;
        this.unclassifiedLibraries = unclassifiedLibraries;
        this.incompatibleLibraries = incompatibleLibraries;
    }

    public MigratedLib[] getValidLibraries() {
        return validLibraries;
    }

    public MigratedLib[] getUpdatedVersions() {
        return updatedVersions;
    }

    public MigratedLib[] getCrossCompatibleLibraries() {
        return crossCompatibleLibraries;
    }

    public MigratedLib[] getIntegratedPlugins() {
        return integratedPlugins;
    }

    public MigratedLib[] getUnclassifiedLibraries() {
        return unclassifiedLibraries;
    }

    public MigratedLib[] getIncompatibleLibraries() {
        return incompatibleLibraries;
    }
}