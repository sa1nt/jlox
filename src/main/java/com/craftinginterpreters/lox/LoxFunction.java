package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.Stmt.Function;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Function declaration;
    private final Environment closure;

    public LoxFunction(Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new ChildEnvironment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).getLexeme(), arguments.get(i));
        }
        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }
        return null;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return String.format("<fn %s >", declaration.name.getLexeme());
    }
}
