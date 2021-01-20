package migrate.interfaces;

public interface ScalacOptions {
    String[] getNotParsed();
    String[] getSpecificScala2();
    String[] getMigrated();
}
