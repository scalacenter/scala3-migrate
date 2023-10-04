package migrate.interfaces;

import java.util.Map;

public class MigratedScalacOptions {
    private String[] valid;
    private Map<String, String> renamed;
    private String[] removed;
    private String[] unknown;

    public MigratedScalacOptions(String[] valid, Map<String, String> renamed, String[] removed, String[] unknown) {
        this.valid = valid;
        this.renamed = renamed;
        this.removed = removed;
        this.unknown = unknown;
    }

    public String[] getValid() {
        return valid;
    }

    public Map<String, String> getRenamed() {
        return renamed;
    }

    public String[] getRemoved() {
        return removed;
    }

    public String[] getUnknown() {
        return unknown;
    }
}
