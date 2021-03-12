package migrate.interfaces;

import java.util.Optional;

public interface Lib {
    String getOrganization();
    String getName();
    String getRevision();
    String getCrossVersion();
    String toString();
    Optional<String> getConfigurations();
    boolean isCompilerPlugin();
}
