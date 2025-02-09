package org.poa.cljt.lex;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PushbackReader;

public class IncrementalReader {
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

    public enum Kind {
        TOKEN,
        STRING,
        NUMBER,
        CHARACTER,
        COMMENT,
        QUOTE,
        UNQUOTE,
        DEREF,
        META,
        SYNTAX_QUOTE,
        LIST_OPEN,
        LIST_CLOSE,
        VECTOR_OPEN,
        VECTOR_CLOSE,
        MAP_OPEN,
        MAP_CLOSE,
        ARG,
        DISPATCH,
        EOF,
        UNKNOWN
    }

    public record Token(
            Kind kind,
            @Nullable String content,
            boolean complete) {
    }


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
        return switch (ch) {
            case '"' -> true;
            case ';' -> true;
            case '\'' -> true;
            case '@' -> true;
            case '^' -> true;
            case '`' -> true;
            case '~' -> true;
            case '(' -> true;
            case ')' -> true;
            case '[' -> true;
            case ']' -> true;
            case '{' -> true;
            case '}' -> true;
            case '\\' -> true;
            case '%' -> true;
            case '#' -> true;
            default -> false;
        };
    }

    private static boolean isTerminatingMacroChar(int ch) {
        return switch (ch) {
            case '"' -> true;
            case ';' -> true;
            case '@' -> true;
            case '^' -> true;
            case '`' -> true;
            case '~' -> true;
            case '(' -> true;
            case ')' -> true;
            case '[' -> true;
            case ']' -> true;
            case '{' -> true;
            case '}' -> true;
            case '\\' -> true;
            default -> false;
        };
    }

    private static boolean isBasicTerminal(int ch) {
        return ch == -1 || ch == ',' || Character.isWhitespace(ch) || isMacroChar(ch);
    }

//    private static boolean isMacroTerminal(int ch) {
//        return ch == -1 || ch == ',' || Character.isWhitespace(ch) || isMacroChar(ch);
//    }

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

    public PendingKind classifyDispatchCharacter(int c) {
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

    private Token readBasicToken(PushbackReader reader, int ch1) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append((char) ch1);

        while (true) {
            int ch = reader.read();
            if (ch == -1 || ch == ',' || Character.isWhitespace(ch) || isTerminatingMacroChar(ch)) {
                reader.unread(ch);
                return new Token(Kind.TOKEN, sb.toString(), true);
            }
            sb.append((char) ch);
        }
    }

    private Token readBasicTokenOrNumber(PushbackReader reader, int c1, int c2) throws IOException {
        reader.unread(c2);
        if (indeterminateCharIsNumber(c1, c2)) {
            return readNumber(reader, c1);
        }
        return readBasicToken(reader, c1);
    }

    private Token readString(PushbackReader reader, int c1) throws IOException {
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
                }
                sb.append((char) c);
            } else {
                sb.append((char) c);
            }

        }

        return new Token(Kind.STRING, sb.toString(), isComplete);
    }

    private Token readNumber(PushbackReader reader, int c1) throws IOException {
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

        return new Token(Kind.NUMBER, sb.toString(), true);
    }

    private Token readCharacter(PushbackReader reader, int c1) throws IOException {
        assert c1 == '\\';
        StringBuilder sb = new StringBuilder();
        sb.append((char) c1);

        while (true) {
            int ch = reader.read();
            if (ch == -1 || ch == ',' || Character.isWhitespace(ch) || isTerminatingMacroChar(ch)) {
                reader.unread(ch);
                return new Token(Kind.CHARACTER, sb.toString(), true);
            }
            sb.append((char) ch);
        }
    }

    private Token readComment(PushbackReader reader, int c1) throws IOException {
        assert c1 == ';';
        StringBuilder sb = new StringBuilder();
        sb.append((char) c1);

        for (int c = reader.read(); c != -1; c = reader.read()) {
            sb.append((char) c);
            if (c == '\n' || c == '\r') {
                break;
            }
        }
        return new Token(Kind.COMMENT, sb.toString(), true);
    }

    private Token readDispatch(PushbackReader reader, int c1) throws IOException {
        assert c1 == '#';
        int nextChar = reader.read();
        if (nextChar == -1) {
            reader.unread(nextChar);
            return new Token(Kind.DISPATCH, null, false);
        }
        if (nextChar == '(' || nextChar == '{') {
            reader.unread(nextChar);
        }

        return new Token(Kind.DISPATCH, Character.toString(nextChar), true);
    }


    private Token readOpen(int c1) {

        return new Token(
                switch (c1) {
                    case '(' -> Kind.LIST_OPEN;
                    case '[' -> Kind.VECTOR_OPEN;
                    case '{' -> Kind.MAP_OPEN;
                    default -> throw new IllegalStateException("Non-open character passed to readOpen: " + c1);
                },
                null, true
        );
    }

    private Token readClose(int c1) {
        return new Token(
                switch (c1) {
                    case ')' -> Kind.LIST_CLOSE;
                    case '}' -> Kind.MAP_CLOSE;
                    case ']' -> Kind.VECTOR_CLOSE;
                    default -> throw new IllegalStateException("Non-open character passed to readOpen: " + c1);
                },
                null, true
        );
    }


    public Token readToken(PushbackReader reader) throws IOException {
        var dispatchChar = advanceToDispatchCharacter(reader);
        var pendingKind = classifyDispatchCharacter(dispatchChar);

        return switch (pendingKind) {
            case TOKEN -> readBasicToken(reader, dispatchChar);
            case TOKEN_OR_NUMBER -> readBasicTokenOrNumber(reader, dispatchChar, reader.read());
            case STRING -> readString(reader, dispatchChar);
            case NUMBER -> readNumber(reader, dispatchChar);
            case CHARACTER -> readCharacter(reader, dispatchChar);
            case COMMENT -> readComment(reader, dispatchChar);
            case QUOTE -> new Token(Kind.QUOTE, null, true);
            case UNQUOTE -> new Token(Kind.UNQUOTE, null, true);
            case DEREF -> new Token(Kind.DEREF, null, true);
            case META -> new Token(Kind.META, null, true);
            case SYNTAX_QUOTE -> new Token(Kind.SYNTAX_QUOTE, null, true);
            case OPEN -> readOpen(dispatchChar);
            case CLOSE -> readClose(dispatchChar);
            case ARG -> new Token(Kind.ARG, null, true);
            case DISPATCH -> readDispatch(reader, dispatchChar);
            case INVALID -> new Token(Kind.UNKNOWN, String.valueOf((char) dispatchChar), true);
            case EOF -> new Token(Kind.EOF, null, true);
        };

    }
}
