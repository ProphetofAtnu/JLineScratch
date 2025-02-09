package org.poa.cljt.lex;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

class IncrementalReaderTest {

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
        IncrementalReader reader = new IncrementalReader();
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
        List<IncrementalReader.Token> tokens = Arrays.asList(
                new IncrementalReader.Token(IncrementalReader.Kind.LIST_OPEN, null, true),
                new IncrementalReader.Token(IncrementalReader.Kind.TOKEN, "this", true),
                new IncrementalReader.Token(IncrementalReader.Kind.STRING, "\"is\"", true),
                new IncrementalReader.Token(IncrementalReader.Kind.TOKEN, "a", true),
                new IncrementalReader.Token(IncrementalReader.Kind.CHARACTER, "\\return", true),
                new IncrementalReader.Token(IncrementalReader.Kind.TOKEN, "test", true),
                new IncrementalReader.Token(IncrementalReader.Kind.NUMBER, "1234", true),
                new IncrementalReader.Token(IncrementalReader.Kind.NUMBER, "+1234", true),
                new IncrementalReader.Token(IncrementalReader.Kind.NUMBER, "-1234", true),
                new IncrementalReader.Token(IncrementalReader.Kind.TOKEN, "+", true),
                new IncrementalReader.Token(IncrementalReader.Kind.TOKEN, "-", true),
                new IncrementalReader.Token(IncrementalReader.Kind.TOKEN, "asdf", true),
                new IncrementalReader.Token(IncrementalReader.Kind.DISPATCH, "{", true),
                new IncrementalReader.Token(IncrementalReader.Kind.MAP_OPEN, null, true),
                new IncrementalReader.Token(IncrementalReader.Kind.MAP_CLOSE, null, true),
                new IncrementalReader.Token(IncrementalReader.Kind.MAP_OPEN, null, true),
                new IncrementalReader.Token(IncrementalReader.Kind.MAP_CLOSE, null, true),
                new IncrementalReader.Token(IncrementalReader.Kind.VECTOR_OPEN, null, true),
                new IncrementalReader.Token(IncrementalReader.Kind.VECTOR_CLOSE, null, true),
                new IncrementalReader.Token(IncrementalReader.Kind.META, null, true),
                new IncrementalReader.Token(IncrementalReader.Kind.TOKEN, "asdf", true),
                new IncrementalReader.Token(IncrementalReader.Kind.QUOTE, null, true),
                new IncrementalReader.Token(IncrementalReader.Kind.TOKEN, "asdf", true),
                new IncrementalReader.Token(IncrementalReader.Kind.SYNTAX_QUOTE, null, true),
                new IncrementalReader.Token(IncrementalReader.Kind.TOKEN, "asdf", true),
                new IncrementalReader.Token(IncrementalReader.Kind.UNQUOTE, null, true),
                new IncrementalReader.Token(IncrementalReader.Kind.TOKEN, "asdf", true),
                new IncrementalReader.Token(IncrementalReader.Kind.UNQUOTE, null, true),
                new IncrementalReader.Token(IncrementalReader.Kind.DEREF, null, true),
                new IncrementalReader.Token(IncrementalReader.Kind.TOKEN, "asdf", true),
                new IncrementalReader.Token(IncrementalReader.Kind.LIST_CLOSE, null, true),
                new IncrementalReader.Token(IncrementalReader.Kind.COMMENT, """
                        ; test
                        """, true),
                new IncrementalReader.Token(IncrementalReader.Kind.STRING, "\"this is", false)
        );
        PushbackReader rdr = new PushbackReader(new StringReader("""
                (this "is" a \\return test 1234 +1234 -1234 + - asdf #{} {} [] ^asdf 'asdf `asdf ~asdf ~@asdf) ; test
                "this is"""));
        IncrementalReader reader = new IncrementalReader();
        IncrementalReader.Token token;
        int ctr = 0;
        while ((token = reader.readToken(rdr)).kind() != IncrementalReader.Kind.EOF) {
            System.out.println(token);
            Assertions.assertEquals(tokens.get(ctr), token);
            ctr++;
        }
        Assertions.assertEquals(IncrementalReader.Kind.EOF, reader.readToken(rdr).kind());
    }

}