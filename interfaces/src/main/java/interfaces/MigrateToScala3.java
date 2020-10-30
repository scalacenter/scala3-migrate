package interfaces;

import java.nio.file.Path;

public interface MigrateToScala3 {

    /**
     * @throws an Exception
     */
    String previewMigration(Path sourceRoot,
                            Path source,
                            Path[] scala2Classpath,
                            String[] scala2CompilerOptions,
                            Path[] toolClasspath,
                            Path[] scala3Classpath,
                            String[] scala3CompilerOptions,
                            Path scala3ClassDirectory);
}
