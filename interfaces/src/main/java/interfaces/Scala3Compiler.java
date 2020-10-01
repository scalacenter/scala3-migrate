package interfaces;

import dotty.tools.dotc.Compiler;
import dotty.tools.dotc.Run;
import dotty.tools.dotc.core.Contexts.Context;
import dotty.tools.dotc.util.SourceFile;
import dotty.tools.dotc.util.SourceFile$;
import scala.collection.immutable.List;
import scala.collection.immutable.List$;
import scala.Function1;
import scala.runtime.AbstractFunction1;
import dotty.tools.dotc.reporting.Reporter;

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

  public void compile(List<CompilationUnit> units) throws CompilationException {
    List<SourceFile> sources = units.map(toSourceFile);
    Reporter reporter = new QuietReporter();
    Context freshContext = context.fresh().setReporter(reporter);
    Run run = compiler.newRun(freshContext);
    run.compileSources(sources);
    if (reporter.hasErrors()) {
      throw new CompilationException();
    }
  }
}
