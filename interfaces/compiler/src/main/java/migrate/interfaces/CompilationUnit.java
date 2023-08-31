package migrate.interfaces;

public class CompilationUnit {
  public String content;
  public String name; 

  public CompilationUnit(String name, String content) {
    this.name = name;
    this.content = content;
  } 
}
