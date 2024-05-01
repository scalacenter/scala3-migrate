package migrate.compiler.interfaces;


import dotty.tools.dotc.Driver;
import dotty.tools.dotc.core.Contexts.Context;
import dotty.tools.dotc.reporting.ConsoleReporter;
import dotty.tools.dotc.reporting.Diagnostic;
import dotty.tools.dotc.reporting.StoreReporter;
import dotty.tools.io.AbstractFile;

import scala.Option;
import scala.PartialFunction;
import scala.Tuple2;
import scala.collection.immutable.List;
import scala.reflect.ClassTag;


public class Scala3Driver extends Driver {
  private static Scala3Driver driver = new Scala3Driver();

  private Scala3Driver() {
    super();
  }

  public boolean sourcesRequired() {
    return false;
  }

  public static Scala3Compiler setupCompiler(String[] args) throws Scala3SetupException {
    return driver.setup(args);
  }

  public Scala3Compiler setup(String[] args) throws Scala3SetupException {
    Context ctx1 = initCtx();
    StoreReporter reporter = new StoreReporter(ctx1.reporter(), false);
    Context ctx2 = ctx1.fresh().setReporter(reporter);
    Option<Tuple2<List<AbstractFile>, Context>> setup = driver.setup(args, initCtx());
    if (reporter.hasErrors()) {
      PartialFunction<Diagnostic, String> errorMessages = new PartialFunction<Diagnostic, String>() {
        public boolean isDefinedAt(Diagnostic diag) {
          return diag.level() == Diagnostic.ERROR;
        }
        public String apply(Diagnostic diag) {
          if (isDefinedAt(diag))
            return diag.message();
          else
            throw new scala.MatchError(diag);
        }
      };
      String message = reporter.removeBufferedMessages(ctx2).collect(errorMessages).head();
      throw new Scala3SetupException(message);
    } else {
      Context ctx3 = setup.get()._2;
      return new Scala3Compiler(newCompiler(ctx3), ctx3);
    }
  }
}
