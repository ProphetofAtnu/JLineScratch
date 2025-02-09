package org.poa.cljt.lex;

import org.jetbrains.annotations.Nullable;

/**
 * Describes a token may or may not be complete, along with a multi-functional content field.
 * <p>
 * The only token structure that can be incomplete is currently IncrementalToken.Kind.STRING,
 * however there are other constructs that may be validated by the tokenizer in the future.
 *
 * @param kind     The token kind
 * @param content  The token content OR dispatcher context
 * @param complete If the token is complete in its current form
 */
public record IncrementalToken(
        Kind kind,
        @Nullable String content,
        boolean complete) {

    public String contentForDisplay() {
        if (content != null) {
            if (kind == Kind.DISPATCH) {
                return "#|" + content;
            }
            return content;
        } else {
            return switch (kind) {
                case DEREF -> "@";
                case EOF -> "<eof>";
                case LIST_OPEN -> "(";
                case LIST_CLOSE -> ")";
                case MAP_OPEN -> "{";
                case MAP_CLOSE -> "}";
                case VECTOR_OPEN -> "[";
                case VECTOR_CLOSE -> "]";
                case META -> "^";
                case QUOTE -> "'";
                case SYNTAX_QUOTE -> "`";
                case UNQUOTE -> "~";
                default -> "<INVALID>";
            };
        }
    }

    @Override
    public String toString() {
        return "IncrementalToken{" +
                "kind=" + kind +
                ", content='" + contentForDisplay() + '\'' +
                ", complete=" + complete +
                '}';
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

}
