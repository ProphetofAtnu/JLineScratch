package org.poa.cljt.lex;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

class IncrementalTokenReaderTest {

    record TestItem<E>(String input, E expected) {
    }

    @org.junit.jupiter.api.Test
    void advanceToDispatchCharacter() {
        TestItem<Integer>[] items = new TestItem[]{
                new TestItem<>("    ()", (int) '('),
                new TestItem<>("""
                        \t\t(     )""", (int) '('),
                new TestItem<>("""
                        (     )""", (int) '('),
                new TestItem<>("""
                        +123""", (int) '+'),
                new TestItem<>("\t\t123", (int) '1'),
                new TestItem<>("asdf", (int) 'a'),
                new TestItem<>("  \"", (int) '"'),
                new TestItem<>("@", (int) '@'),
                new TestItem<>("^", (int) '^'),
                new TestItem<>("`", (int) '`'),
                new TestItem<>("~", (int) '~'),
                new TestItem<>("(", (int) '('),
                new TestItem<>(")", (int) ')'),
                new TestItem<>("[", (int) '['),
                new TestItem<>("]", (int) ']'),
                new TestItem<>("{", (int) '{'),
                new TestItem<>("}", (int) '}'),
                new TestItem<>("\\", (int) '\\'),
                new TestItem<>("%", (int) '%'),
                new TestItem<>("#", (int) '#'),
        };
        IncrementalTokenReader reader = new IncrementalTokenReader();
        for (var item : items) {
            PushbackReader rdr = new PushbackReader(new StringReader(item.input));
            try {
                Assertions.assertEquals(reader.advanceToDispatchCharacter(rdr), item.expected);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void readToken() throws IOException {
        List<IncrementalToken> tokens = Arrays.asList(
                new IncrementalToken(IncrementalToken.Kind.LIST_OPEN, null, true),
                new IncrementalToken(IncrementalToken.Kind.TOKEN, "this", true),
                new IncrementalToken(IncrementalToken.Kind.STRING, "\"is\"", true),
                new IncrementalToken(IncrementalToken.Kind.TOKEN, "a", true),
                new IncrementalToken(IncrementalToken.Kind.CHARACTER, "\\return", true),
                new IncrementalToken(IncrementalToken.Kind.TOKEN, "test", true),
                new IncrementalToken(IncrementalToken.Kind.NUMBER, "1234", true),
                new IncrementalToken(IncrementalToken.Kind.NUMBER, "+1234", true),
                new IncrementalToken(IncrementalToken.Kind.NUMBER, "-1234", true),
                new IncrementalToken(IncrementalToken.Kind.TOKEN, "+", true),
                new IncrementalToken(IncrementalToken.Kind.TOKEN, "-", true),
                new IncrementalToken(IncrementalToken.Kind.TOKEN, "asdf", true),
                new IncrementalToken(IncrementalToken.Kind.DISPATCH, "{", true),
                new IncrementalToken(IncrementalToken.Kind.MAP_OPEN, null, true),
                new IncrementalToken(IncrementalToken.Kind.MAP_CLOSE, null, true),
                new IncrementalToken(IncrementalToken.Kind.MAP_OPEN, null, true),
                new IncrementalToken(IncrementalToken.Kind.MAP_CLOSE, null, true),
                new IncrementalToken(IncrementalToken.Kind.VECTOR_OPEN, null, true),
                new IncrementalToken(IncrementalToken.Kind.VECTOR_CLOSE, null, true),
                new IncrementalToken(IncrementalToken.Kind.META, null, true),
                new IncrementalToken(IncrementalToken.Kind.TOKEN, "asdf", true),
                new IncrementalToken(IncrementalToken.Kind.QUOTE, null, true),
                new IncrementalToken(IncrementalToken.Kind.TOKEN, "asdf", true),
                new IncrementalToken(IncrementalToken.Kind.SYNTAX_QUOTE, null, true),
                new IncrementalToken(IncrementalToken.Kind.TOKEN, "asdf", true),
                new IncrementalToken(IncrementalToken.Kind.UNQUOTE, null, true),
                new IncrementalToken(IncrementalToken.Kind.TOKEN, "asdf", true),
                new IncrementalToken(IncrementalToken.Kind.UNQUOTE, null, true),
                new IncrementalToken(IncrementalToken.Kind.DEREF, null, true),
                new IncrementalToken(IncrementalToken.Kind.TOKEN, "asdf", true),
                new IncrementalToken(IncrementalToken.Kind.LIST_CLOSE, null, true),
                new IncrementalToken(IncrementalToken.Kind.COMMENT, """
                        ; test
                        """, true),
                new IncrementalToken(IncrementalToken.Kind.STRING, "\"this is", false)
        );
        PushbackReader rdr = new PushbackReader(new StringReader("""
                (this "is" a \\return test 1234 +1234 -1234 + - asdf #{} {} [] ^asdf 'asdf `asdf ~asdf ~@asdf) ; test
                "this is"""));
        IncrementalTokenReader reader = new IncrementalTokenReader();
        IncrementalToken token;
        int ctr = 0;
        while ((token = reader.readToken(rdr)).kind() != IncrementalToken.Kind.EOF) {
            System.out.println(token);
            Assertions.assertEquals(tokens.get(ctr), token);
            ctr++;
        }
        Assertions.assertEquals(IncrementalToken.Kind.EOF, reader.readToken(rdr).kind());
    }

}