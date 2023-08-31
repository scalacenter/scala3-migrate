package migrate.compiler.interfaces;

import dotty.tools.dotc.core.Contexts.Context;
import dotty.tools.dotc.reporting.Diagnostic;
import dotty.tools.dotc.reporting.Reporter;

import java.util.HashSet;

// we want to get the paths of all files that have errors 
public class FileWithErrorReporter extends Reporter {
  HashSet<String> filesWithErrors = new HashSet<String>();
  
  public void doReport(Diagnostic dia, Context ctx) {
    if (dia.level() == Diagnostic.ERROR) {
      String filePath = dia.pos().source().path();
      filesWithErrors.add(filePath);
    }
  }
  
  public String[] getFilesWithErrors() {
    return filesWithErrors.stream().toArray(String[]::new);
  }
}
