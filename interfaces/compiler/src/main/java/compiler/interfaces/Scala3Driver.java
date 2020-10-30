package compiler.interfaces;

import dotty.tools.dotc.Driver;
import dotty.tools.dotc.core.Contexts.Context;

public class Scala3Driver extends Driver {
  private static Scala3Driver driver = new Scala3Driver();

  private Scala3Driver() {
    super();
  }

  public boolean sourcesRequired() {
    return false;
  }

  public static Scala3Compiler setupCompiler(String[] args) {
    return driver.setup(args);
  }

  public Scala3Compiler setup(String[] args) {
    Context setup = driver.setup(args, initCtx())._2;
    return new Scala3Compiler(newCompiler(setup), setup);
  }
}
