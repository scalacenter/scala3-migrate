package migrate.compiler.interfaces;

import java.nio.file.Path;

public class CompilationUnit {
  public String content;
  public String name;
  public Path path;

  public CompilationUnit(String name, String content, Path path) {
    this.name = name;
    this.content = content;
    this.path = path;
  } 
}
