package compiler.interfaces;

import dotty.tools.dotc.reporting.*;
import dotty.tools.dotc.core.Contexts.*;


// a copy of DelegatingReporter
// https://github.com/lampepfl/dotty/blob/b9b1f2a083c4b3ef130fbb70c7da1956b144e2a1/sbt-bridge/src/xsbt/DelegatingReporter.java
public class MigrateReporter extends AbstractReporter {
    
    private final Logger logger;
    
    public MigrateReporter(Logger logger) {
        super();
        this.logger = logger;
    }
            
    public void doReport(dotty.tools.dotc.reporting.Diagnostic dia, Context ctx) {

        if (dia.level() == Diagnostic.ERROR) {
            Message message = dia.msg();
            StringBuilder rendered = new StringBuilder();
            rendered.append(messageAndPos(dia, ctx));

            boolean shouldExplain = dotty.tools.dotc.reporting.Diagnostic.shouldExplain(dia, ctx);
            if (shouldExplain && !message.explanation().isEmpty()) {
                rendered.append(explanation(message, ctx));
            };
            logger.error(rendered.toString());
        }
    }
}
