package migrate.interfaces;

import java.nio.file.Path;
import java.util.List;

public interface MigrateService {

    void migrate(Path sourceRoot, Path workspce, List<Path> scala2Classpath, List<String> scala2CompilerOptions,
            List<Path> toolClasspath, List<Path> scala3Classpath, List<String> scala3CompilerOptions,
            Path scala3ClassDirectory);
}
