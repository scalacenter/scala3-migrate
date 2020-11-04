package interfaces;

import dotty.tools.dotc.reporting.Diagnostic.Error;

class CompilationException extends Exception {
    public CompilationException(String message){
        super(message);
    }
}