package migrate.compiler.interfaces;

import dotty.tools.dotc.util.SourceFile;
import java.nio.file.Path;

class MigrationSourceFile extends SourceFile {
  private char[] _content;

  public MigrationSourceFile(String content, String name, Path path) {
    super(new MigrationFile(name, path), null);
    this._content = content.toCharArray();
  }

  @Override
  public char[] content() {
    return _content;
  }
}
