package migrate.interfaces;

public interface Lib {
    String getOrganization();
    String getName();
    String getRevision();
    String getCrossVersion();
    String toString();
    boolean isCompilerPlugin();
}
