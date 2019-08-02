package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class ChildEnvironment implements Environment {
    private final Environment enclosingEnvironment;
    private final Map<String, Object> values = new HashMap<>();

    ChildEnvironment(Environment enclosingEnvironment) {
        this.enclosingEnvironment = enclosingEnvironment;
    }

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

        return enclosingEnvironment.get(name);
    }

    @Override
    public void assign(Token name, Object value) {
        String varName = name.getLexeme();
        if (values.containsKey(varName)) {
            values.put(varName, value);
            return;
        }

        enclosingEnvironment.assign(name, value);
    }
}
