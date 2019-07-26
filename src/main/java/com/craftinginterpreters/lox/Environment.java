package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();

    void define(String name, Object value) {
        values.put(name, value);
    }

    Object get(Token name) {
        String varName = name.getLexeme();
        if (values.containsKey(varName)) {
            return values.get(varName);
        }
        throw new LoxRuntimeError(name, "Undefinded variable " + varName);
    }
}
