package migrate.interfaces;

import java.util.Map;


// idee
public interface MigratedLib {
    boolean isCompatibleWithScala3();
    String toString();
    String getReasonWhy();
}