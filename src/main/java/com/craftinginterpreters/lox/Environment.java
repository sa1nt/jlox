package com.craftinginterpreters.lox;

public interface Environment {
    void define(String name, Object value);
    Object get(Token name);
    void assign(Token name, Object value);
}
