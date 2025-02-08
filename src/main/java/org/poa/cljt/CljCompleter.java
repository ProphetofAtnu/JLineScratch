package org.poa.cljt;

import clojure.lang.*;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CljCompleter implements Completer {
    final static private Symbol CLOJURE_REPL = Symbol.intern("clojure.repl");

    final static private Var APROPOS = RT.var("clojure.repl", "apropos");

    public static void requireDeps(@NotNull IFn requireFn) {
        requireFn.invoke(CLOJURE_REPL);
    }

    public ArrayList<Symbol> apropos(String input) {
        ArrayList<Symbol> candidates = new ArrayList<>();

        for (@SuppressWarnings("rawtypes") Iterator it = RT.iter(APROPOS.invoke(input)); it.hasNext(); ) {
            var itm = it.next();
            if (itm instanceof Symbol sym) {
                candidates.add(sym);
            }
        }

        return candidates;
    }

    private static Candidate convertSymbol(Symbol sym) {
        return new Candidate(sym.getName(), sym.getName(), sym.getNamespace(), null, sym.getNamespace(), null, true);
    }

    public void aproposCandidates(String input, List<Candidate> outList) {
        for (@SuppressWarnings("rawtypes") Iterator it = RT.iter(APROPOS.invoke(input)); it.hasNext(); ) {
            var itm = it.next();
            if (itm instanceof Symbol sym) {
                outList.add(CljCompleter.convertSymbol(sym));
            }
        }

    }

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> list) {
        aproposCandidates(parsedLine.word(), list);
    }
}
