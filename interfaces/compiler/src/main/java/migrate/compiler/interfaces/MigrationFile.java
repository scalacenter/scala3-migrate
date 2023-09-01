package migrate.compiler.interfaces;

import dotty.tools.io.VirtualFile;
import java.nio.file.Path;

class MigrationFile extends VirtualFile {
  private Path _jpath;

  public MigrationFile(String name, Path path) {
    super(name, path.toString());
    this._jpath = path; 
  }

  @Override
  public Path jpath() {
    return _jpath;
  }
  
}
