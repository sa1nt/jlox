package com.craftinginterpreters.lox;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode
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

    TokenType getType() {
        return type;
    }

    String getLexeme() {
        return lexeme;
    }

    public Object getLiteral() {
        return literal;
    }

    int getLine() {
        return line;
    }

    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal + " on line " + line;
    }
}
