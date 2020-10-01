package interfaces;

import dotty.tools.dotc.reporting.Reporter;
import dotty.tools.dotc.core.Contexts.Context;
import dotty.tools.dotc.reporting.Diagnostic;

public class QuietReporter extends Reporter {
  public void doReport(Diagnostic d, Context ctx) {}
}
