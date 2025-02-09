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

    @Test
    void advanceToDispatchCharacter() {
        var items = Arrays.asList(
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
                new TestItem<>("#", (int) '#')
        );
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
        /*
        This was written by hand to ensure that it behaved as it logically, and it was super annoying.

        I tried to include as many "gotcha" style problems as I could while keeping it as a valid clojure form. The only
        section that will not parse in clojure is the second line, intentionally. The first line is valid a valid
        Clojure (as of 1.12.0) form with a comment:

        ```
        (this "is" a \return test 1234 +1234 -1234 + - asdf #{} {} [] asdf^asdf 'asdf `asdf ~asdf ~@asdf) ; test
        ```

        Which expands to:
        ```
        (this "is" a \return test 1234 1234 -1234 + - asdf #{} {} [] asdf
          (quote asdf) (quote user/asdf) (clojure.core/unquote asdf)
          (clojure.core/unquote-splicing asdf))
        ```

        More tests may be added if other partial token constructs are used.
        */
        var input = """
                (this "is" a \\return test 1234 +1234 -1234 + - asdf #{} {} [] asdf^asdf 'asdf `asdf ~asdf ~@asdf) ; test
                "this is""";
        System.out.println(input);
        PushbackReader rdr = new PushbackReader(new StringReader(input));

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
                new IncrementalToken(IncrementalToken.Kind.TOKEN, "asdf", true),
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
        long[] advances = new long[]{
                1, 5, 10, 12, 20, 25, 30, 36, 42, 44,
                46, 51, 53, 54, 55, 57, 58, 60, 61,
                66, 67, 71, 73, 77, 79, 83, 85, 89,
                91, 92, 96, 97, 105, 113
        };
        IncrementalTokenReader reader = new IncrementalTokenReader();
        IncrementalToken token;
        int ctr = 0;
        while ((token = reader.readToken(rdr)).kind() != IncrementalToken.Kind.EOF) {
            var adv = reader.getAdvance();
            System.out.println(token);
            System.out.println(adv);
            Assertions.assertEquals(tokens.get(ctr), token);
            Assertions.assertEquals(advances[ctr], adv);
            Assertions.assertEquals(input.substring((int) (adv - token.actualTokenLength()), (int) adv), token.contentOrRepr());
            ctr++;
        }
        Assertions.assertEquals(IncrementalToken.Kind.EOF, reader.readToken(rdr).kind());
        Assertions.assertEquals(input.length(), reader.getAdvance());

        reader.resetState();

        Assertions.assertEquals(0, reader.getAdvance());

    }

}