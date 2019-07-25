package com.craftinginterpreters.lox;

public class LoxRuntimeError extends RuntimeException {
    final Token token;

    public LoxRuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
