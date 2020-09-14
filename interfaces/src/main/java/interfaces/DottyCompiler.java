package interfaces;

import dotty.tools.dotc.Main;

public class DottyCompiler {
    public static void compile(String[] args) {
        Main.process(args);
    }
}