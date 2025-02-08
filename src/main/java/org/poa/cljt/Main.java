package org.poa.cljt;


import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
    final static private Symbol CLOJURE_REPL = Symbol.intern("clojure.repl");
    final static private Var REQUIRE = RT.var("clojure.core", "require");
    final static private Var APROPOS = RT.var("clojure.repl", "apropos");

    static Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws IOException {
        LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));

        CljCompleter.requireDeps(REQUIRE);
        var complete = new CljCompleter();

        try (var terminal = TerminalBuilder.terminal()) {
            var lreader = LineReaderBuilder.builder()
                    .appName("demo")
                    .terminal(terminal)
                    .completer(new CljCompleter())
                    .build();

            var lne = lreader.readLine("demo> ");
            REQUIRE.invoke(CLOJURE_REPL);

            var out = APROPOS.invoke(lne);
            System.out.println(out);
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
    }
}