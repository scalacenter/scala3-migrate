package migrate.interfaces;

import java.util.Map;

public interface ScalacOptions {
    String[] getNotParsed();
    String[] getSpecificScala2();
    String[] getScala3cOptions();
    Map<String, String> getRenamed();
}
