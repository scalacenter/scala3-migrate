package compiler.interfaces;

import dotty.tools.dotc.core.Contexts.Context;
import dotty.tools.dotc.reporting.Diagnostic;
import dotty.tools.dotc.reporting.Reporter;

public class QuietReporter extends Reporter {
  public void doReport(Diagnostic d, Context ctx) {}
}
