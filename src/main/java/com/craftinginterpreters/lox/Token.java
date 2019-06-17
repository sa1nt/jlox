package com.craftinginterpreters.lox;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

class Token {
    private final TokenType type;
    private final String lexeme;
    private final Object literal;
    private final int line;

    Token(TokenType type, String lexeme, @Nullable Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String getLexeme() {
        return lexeme;
    }

    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal + " on line " + line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return line == token.line &&
                type == token.type &&
                lexeme.equals(token.lexeme) &&
                Objects.equals(literal, token.literal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, lexeme, literal, line);
    }
}
