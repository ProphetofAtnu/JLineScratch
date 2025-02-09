package org.poa.cljt.lex;

import java.io.IOException;
import java.io.PushbackReader;

/**
 * Used to tokenize a potentially incomplete form.
 * This IncrementalReader only performs the tokenization task,
 * after which the tokens must be validated for correctness.
 * <p>
 * This class is currently stateless, however it may be worth keeping a
 * state at some point, so I'm only making a subset of the utility functions
 * static.
 */
public class IncrementalTokenReader {


    private static final PendingKind[] macroCharTable = new PendingKind[256];

    static {
        macroCharTable['+'] = PendingKind.TOKEN_OR_NUMBER;
        macroCharTable['-'] = PendingKind.TOKEN_OR_NUMBER;

        macroCharTable['"'] = PendingKind.STRING;
        macroCharTable[';'] = PendingKind.COMMENT;
        macroCharTable['\''] = PendingKind.QUOTE;
        macroCharTable['@'] = PendingKind.DEREF;
        macroCharTable['^'] = PendingKind.META;
        macroCharTable['`'] = PendingKind.SYNTAX_QUOTE;
        macroCharTable['~'] = PendingKind.UNQUOTE;
        macroCharTable['('] = PendingKind.OPEN;
        macroCharTable[')'] = PendingKind.CLOSE;
        macroCharTable['['] = PendingKind.OPEN;
        macroCharTable[']'] = PendingKind.CLOSE;
        macroCharTable['{'] = PendingKind.OPEN;
        macroCharTable['}'] = PendingKind.CLOSE;
        macroCharTable['\\'] = PendingKind.CHARACTER;
        macroCharTable['%'] = PendingKind.ARG;
        macroCharTable['#'] = PendingKind.DISPATCH;

        macroCharTable['0'] = PendingKind.NUMBER;
        macroCharTable['1'] = PendingKind.NUMBER;
        macroCharTable['2'] = PendingKind.NUMBER;
        macroCharTable['3'] = PendingKind.NUMBER;
        macroCharTable['4'] = PendingKind.NUMBER;
        macroCharTable['5'] = PendingKind.NUMBER;
        macroCharTable['6'] = PendingKind.NUMBER;
        macroCharTable['7'] = PendingKind.NUMBER;
        macroCharTable['8'] = PendingKind.NUMBER;
        macroCharTable['9'] = PendingKind.NUMBER;
    }

    private static boolean isMacroChar(int ch) {
        return !Character.isDigit(ch) && ch != '+' && ch != '-'
                && ch < macroCharTable.length && macroCharTable[ch] != null;
    }

    private static boolean isTerminatingMacroChar(int ch) {
        return ch != '#' && ch != '\'' && ch != '%' && isMacroChar(ch);
    }

    private static boolean isBasicTerminal(int ch) {
        return ch == -1 || ch == ',' || Character.isWhitespace(ch) || isMacroChar(ch);
    }

    public int advanceToDispatchCharacter(PushbackReader reader) throws IOException {
        while (true) {
            int c = reader.read();
            if (c == -1) {
                return c;
            }

            if (c == ',' || Character.isWhitespace(c)) {
                continue;
            }

            return c;
        }
    }

    private PendingKind classifyDispatchCharacter(int c) {
        if (c == -1) {
            return PendingKind.EOF;
        }
        if (c == ',' || Character.isWhitespace(c)) {
            return PendingKind.INVALID;
        }

        if (c < macroCharTable.length) {
            if (macroCharTable[c] == null) {
                return PendingKind.TOKEN;
            }
            return macroCharTable[c];
        }
        return PendingKind.TOKEN;
    }

    private boolean indeterminateCharIsNumber(int ch1, int ch2) {
        assert ch1 == '+' || ch1 == '-';
        return Character.isDigit(ch2);
    }

    private IncrementalToken readBasicToken(PushbackReader reader, int ch1) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append((char) ch1);

        while (true) {
            int ch = reader.read();
            if (ch == -1 || ch == ',' || Character.isWhitespace(ch) || isTerminatingMacroChar(ch)) {
                reader.unread(ch);
                return new IncrementalToken(IncrementalToken.Kind.TOKEN, sb.toString(), true);
            }
            sb.append((char) ch);
        }
    }

    private IncrementalToken readBasicTokenOrNumber(PushbackReader reader, int c1, int c2) throws IOException {
        reader.unread(c2);
        if (indeterminateCharIsNumber(c1, c2)) {
            return readNumber(reader, c1);
        }
        return readBasicToken(reader, c1);
    }

    private IncrementalToken readString(PushbackReader reader, int c1) throws IOException {
        assert c1 == '"';
        boolean isComplete = false;
        StringBuilder sb = new StringBuilder();
        sb.append((char) c1);
        for (int c = reader.read(); c != -1; c = reader.read()) {
            if (c == '"') {
                isComplete = true;
                sb.append((char) c);
                break;
            } else if (c == '\\') {
                sb.append((char) c);
                c = reader.read();
                if (c == -1) {
                    reader.unread(c);

                    break;
                } else {
                    sb.append((char) c);
                }
            } else {
                sb.append((char) c);
            }

        }

        return new IncrementalToken(IncrementalToken.Kind.STRING, sb.toString(), isComplete);
    }

    private IncrementalToken readNumber(PushbackReader reader, int c1) throws IOException {
        assert Character.isDigit(c1) || c1 == '+' || c1 == '-';

        StringBuilder sb = new StringBuilder();
        sb.append((char) c1);

        while (true) {
            int ch = reader.read();
            if (isBasicTerminal(ch)) {
                reader.unread(ch);
                break;
            }
            sb.append((char) ch);
        }

        return new IncrementalToken(IncrementalToken.Kind.NUMBER, sb.toString(), true);
    }

    private IncrementalToken readCharacter(PushbackReader reader, int c1) throws IOException {
        assert c1 == '\\';
        StringBuilder sb = new StringBuilder();
        sb.append((char) c1);

        while (true) {
            int ch = reader.read();
            if (ch == -1 || ch == ',' || Character.isWhitespace(ch) || isTerminatingMacroChar(ch)) {
                reader.unread(ch);
                return new IncrementalToken(IncrementalToken.Kind.CHARACTER, sb.toString(), true);
            }
            sb.append((char) ch);
        }
    }

    private IncrementalToken readComment(PushbackReader reader, int c1) throws IOException {
        assert c1 == ';';
        StringBuilder sb = new StringBuilder();
        sb.append((char) c1);

        for (int c = reader.read(); c != -1; c = reader.read()) {
            sb.append((char) c);
            if (c == '\n' || c == '\r') {
                break;
            }
        }
        return new IncrementalToken(IncrementalToken.Kind.COMMENT, sb.toString(), true);
    }

    private IncrementalToken readDispatch(PushbackReader reader, int c1) throws IOException {
        assert c1 == '#';
        int nextChar = reader.read();
        if (nextChar == -1) {
            reader.unread(nextChar);
            return new IncrementalToken(IncrementalToken.Kind.DISPATCH, null, false);
        }
        if (nextChar == '(' || nextChar == '{') {
            reader.unread(nextChar);
        }

        return new IncrementalToken(IncrementalToken.Kind.DISPATCH, Character.toString(nextChar), true);
    }


    private IncrementalToken readOpen(int c1) {

        return new IncrementalToken(
                switch (c1) {
                    case '(' -> IncrementalToken.Kind.LIST_OPEN;
                    case '[' -> IncrementalToken.Kind.VECTOR_OPEN;
                    case '{' -> IncrementalToken.Kind.MAP_OPEN;
                    default -> throw new IllegalStateException("Non-open character passed to readOpen: " + c1);
                },
                null, true
        );
    }

    private IncrementalToken readClose(int c1) {
        return new IncrementalToken(
                switch (c1) {
                    case ')' -> IncrementalToken.Kind.LIST_CLOSE;
                    case '}' -> IncrementalToken.Kind.MAP_CLOSE;
                    case ']' -> IncrementalToken.Kind.VECTOR_CLOSE;
                    default -> throw new IllegalStateException("Non-open character passed to readOpen: " + c1);
                },
                null, true
        );
    }


    /**
     * Advances a PushbackReader and extracts an IncrementalToken object.
     *
     * @param reader the source reader
     * @return The token
     */
    public IncrementalToken readToken(PushbackReader reader) throws IOException {
        var dispatchChar = advanceToDispatchCharacter(reader);
        var pendingKind = classifyDispatchCharacter(dispatchChar);

        return switch (pendingKind) {
            case TOKEN -> readBasicToken(reader, dispatchChar);
            case TOKEN_OR_NUMBER -> readBasicTokenOrNumber(reader, dispatchChar, reader.read());
            case STRING -> readString(reader, dispatchChar);
            case NUMBER -> readNumber(reader, dispatchChar);
            case CHARACTER -> readCharacter(reader, dispatchChar);
            case COMMENT -> readComment(reader, dispatchChar);
            case QUOTE -> new IncrementalToken(IncrementalToken.Kind.QUOTE, null, true);
            case UNQUOTE -> new IncrementalToken(IncrementalToken.Kind.UNQUOTE, null, true);
            case DEREF -> new IncrementalToken(IncrementalToken.Kind.DEREF, null, true);
            case META -> new IncrementalToken(IncrementalToken.Kind.META, null, true);
            case SYNTAX_QUOTE -> new IncrementalToken(IncrementalToken.Kind.SYNTAX_QUOTE, null, true);
            case OPEN -> readOpen(dispatchChar);
            case CLOSE -> readClose(dispatchChar);
            case ARG -> new IncrementalToken(IncrementalToken.Kind.ARG, null, true);
            case DISPATCH -> readDispatch(reader, dispatchChar);
            case INVALID ->
                    new IncrementalToken(IncrementalToken.Kind.UNKNOWN, String.valueOf((char) dispatchChar), true);
            case EOF -> new IncrementalToken(IncrementalToken.Kind.EOF, null, true);
        };

    }

    public enum PendingKind {
        TOKEN,
        TOKEN_OR_NUMBER,
        STRING,
        NUMBER,
        CHARACTER,
        COMMENT,
        QUOTE,
        UNQUOTE,
        DEREF,
        META,
        SYNTAX_QUOTE,
        OPEN,
        CLOSE,
        ARG,
        DISPATCH,
        INVALID,
        EOF
    }
}
