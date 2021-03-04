package compiler.interfaces;

import dotty.tools.dotc.Compiler;
import dotty.tools.dotc.Run;
import dotty.tools.dotc.core.Contexts.Context;
import dotty.tools.dotc.reporting.Reporter;
import dotty.tools.dotc.util.SourceFile;
import dotty.tools.dotc.util.SourceFile$;
import scala.Function1;
import scala.collection.immutable.List;
import scala.runtime.AbstractFunction1;

import java.util.HashSet;

public class Scala3Compiler {
  private Compiler compiler;
  private Context context;

  private Function1<CompilationUnit, SourceFile> toSourceFile = new AbstractFunction1<CompilationUnit, SourceFile>() {
    public SourceFile apply(CompilationUnit unit) {
      return SourceFile$.MODULE$.virtual(unit.name, unit.content, false);
    }
  };

  public Scala3Compiler(Compiler compiler, Context context) {
    this.compiler = compiler;
    this.context = context;
  }

  public static Scala3Compiler setup(String[] args) {
    return Scala3Driver.setupCompiler(args);
  }

  public void quietCompile(List<CompilationUnit> units) throws CompilationException {
    List<SourceFile> sources = units.map(toSourceFile);
    Reporter reporter = new QuietReporter();
    Context freshContext = context.fresh().setReporter(reporter);
    Run run = compiler.newRun(freshContext);
    run.compileSources(sources);
    if (reporter.hasErrors()) {
      throw new CompilationException(reporter.allErrors().mkString("\n"));
    }
  }
  
  public void compileAndReport(List<CompilationUnit> units, Logger r) throws CompilationException {
    List<SourceFile> sources = units.map(toSourceFile);
    Reporter reporter = new MigrateReporter(r);
    Context freshContext = context.fresh().setReporter(reporter);
    Run run = compiler.newRun(freshContext);
    run.compileSources(sources);
    if (reporter.hasErrors()) {
      throw new CompilationException(reporter.allErrors().mkString("\n"));
    }
  }

  public String[] compileAndReportFilesWithErrors(List<CompilationUnit> units) {
    List<SourceFile> sources = units.map(toSourceFile);
    FileWithErrorReporter reporter = new FileWithErrorReporter();
    Context freshContext = context.fresh().setReporter(reporter);
    Run run = compiler.newRun(freshContext);
    run.compileSources(sources);
    return reporter.getFilesWithErrors();
  }
}
