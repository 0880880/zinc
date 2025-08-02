package org.zinc_lang;

import com.github.zeroeighteightzero.lwlp.Token;

import java.util.ArrayDeque;
import java.util.List;

public class Parser {

    public static boolean DEBUG = true;

    public static class Section implements AutoCloseable {
        private final Parser parser;

        public Section(Parser parser) {
            this.parser = parser;
        }

        @Override
        public void close() {
            if (Parser.DEBUG) {
                parser.debugHeight--;
                System.out.printf(" ".repeat(parser.debugHeight * 4) + "  </%s>\n", parser.debugStack.pop());
            }
        }
    }

    public int idx;
    private final List<Token> toks;
    private final ArrayDeque<Integer> snapshots;
    private final ArrayDeque<String> errors;
    public String type;
    public String value;
    public String source;
    public int sourceStart;
    public int sourceEnd;

    public Parser(int idx, List<Token> toks) {
        this.snapshots = new ArrayDeque<>();
        this.errors = new ArrayDeque<>();
        this.toks = toks;
        this.idx = idx - 1;
        this.advance();
    }

    /**
     * Takes a snapshot of the current state.
     *
     * @return This parser/"token"
     */
    public Parser snap() {
        this.snapshots.push(this.idx);
        return this;
    }

    /**
     * Returns the state of the parser back to the captured snapshot if one exists.
     *
     * @return This parser/"token"
     */
    public Parser shot() {
        if (this.snapshots.isEmpty())
            return this;
        this.idx = this.snapshots.pop() - 1;
        this.advance();
        return this;
    }

    /**
     * Advance to the next token.
     *
     * @return This parser/"token"
     */
    public Parser advance() {
        idx++;
        if (idx >= toks.size()) {
            this.type = null;
            this.value = null;
            return this;
        }
        Token tok = toks.get(idx);
        this.type = tok.type;
        this.value = tok.value;
        this.sourceStart = tok.sourceStart;
        this.sourceEnd = tok.sourceEnd;
        return this;
    }

    /**
     * Has the parser ended.
     *
     * @return true if the last token was consumed.
     */
    public boolean end() {
        return this.type == null;
    }

    /**
     * Checks if the type of the current token is equal to input
     *
     * @param type the type to check against.
     * @return Whether the match was truthy.
     */
    public boolean is(String type) {
        if (this.type == null) {
            return false;
        }
        return this.type.equals(type);
    }

    /**
     * Checks if the type of the current token matches any of the type(s). (Logical OR)
     *
     * @param types the type(s) to check against.
     * @return Whether the match was truthy.
     */
    public boolean is(String... types) {
        if (this.type == null) {
            return false;
        }
        for (String s : types) {
            if (this.type.equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The real current token.
     *
     * @return An LWLP Token.
     */
    public Token cur() {
        return toks.get(idx);
    }

    /**
     * Queues an error with the specified message.
     *
     * @param message The error message.
     * @return This parser/"token"
     */
    public Parser error(String message) {

        int start = 0;
        int end = source.length();
        int lineNumber = 1;

        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                if (i < sourceStart) {
                    start = i + 1;
                } else {
                    end = i;
                    break;
                }
                lineNumber++;
            }
        }

        String line = source.substring(start, end);

        int relativeSourceStart = sourceStart - start;
        int markingWidth = sourceEnd - sourceStart;
        String formattedError = String.format(
                "%4d  %s\n%s%s\n\n Error at L%d : %s\n",
                lineNumber,
                line,
                " ".repeat(relativeSourceStart + 6),
                "^".repeat(markingWidth),
                lineNumber,
                message
        );

        errors.offer(formattedError);

        return this;
    }

    private int debugHeight = 0;
    private final ArrayDeque<String> debugStack = new ArrayDeque<>();

    public Section debug(String source) {
        if (DEBUG) {
            debugStack.push(source);
            System.out.printf(" ".repeat(debugHeight * 4) + " <%s>   {%s}  #%d\n", source, this, idx);
            debugHeight++;
            if (debugHeight >= 50) {
                System.err.println("LIMIT REACHED");
                System.exit(1);
            }
        }
        return new Section(this);
    }

    public final Section debug(String source, String message) {
        if (DEBUG) {
            System.out.printf(" ".repeat(debugHeight * 4) + " [%s] | {%s}  #%d   : %s\n", source, this, idx, message);
            debugHeight++;
        }
        return new Section(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof String) {
            return this.value.equals(o);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("T<%s>(\"%s\")", type, value);
    }

}
