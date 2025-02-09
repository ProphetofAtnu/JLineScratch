package org.poa.cljt;

import clojure.java.api.Clojure;
import clojure.lang.LispReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.SyntaxError;
import org.jline.reader.impl.DefaultParser;

import java.io.PushbackReader;
import java.io.StringReader;
import java.util.List;

public class CljParser implements Parser {
    private final Parser inner = new DefaultParser();

    class CljParsedLine implements ParsedLine {

        @Override
        public String word() {
            return "";
        }

        @Override
        public int wordCursor() {
            return 0;
        }

        @Override
        public int wordIndex() {
            return 0;
        }

        @Override
        public List<String> words() {
            return List.of();
        }

        @Override
        public String line() {
            return "";
        }

        @Override
        public int cursor() {
            return 0;
        }
    }


//    private String readToken(PushbackReader reader) {
//        StringBuilder sb = new StringBuilder();
//    }

    @Override
    public ParsedLine parse(String s, int i, ParseContext parseContext) throws SyntaxError {
        var reader = new PushbackReader(new StringReader(s));
        var exp = LispReader.read(reader, false, null, false);
        System.out.println(exp);
        return inner.parse(s, i, parseContext);
    }
}
