package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class RootEnvironment implements Environment {
    private final Map<String, Object> values = new HashMap<>();

    @Override
    public void define(String name, Object value) {
        values.put(name, value);
    }

    @Override
    public Object get(Token name) {
        String varName = name.getLexeme();
        if (values.containsKey(varName)) {
            return values.get(varName);
        }

        throw new LoxRuntimeError(name, "Undefinded variable " + varName);
    }

    @Override
    public void assign(Token name, Object value) {
        String varName = name.getLexeme();
        if (values.containsKey(varName)) {
            values.put(varName, value);
            return;
        }

        throw new LoxRuntimeError(name, "Undefined variable " + varName);
    }
}
