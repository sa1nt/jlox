package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{

    Environment currentEnvironment = new RootEnvironment();

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (LoxRuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new ChildEnvironment(currentEnvironment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, this.currentEnvironment);
        currentEnvironment.define(stmt.name.getLexeme(), function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Expr initializer = stmt.initializer;

        Object value = Objects.nonNull(initializer) ?
                evaluate(initializer) :
                VariableUninitialized.UNINITIALIZED;

        currentEnvironment.define(stmt.name.getLexeme(), value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        try {
            while (isTruthy(evaluate(stmt.condition))) {
                execute(stmt.body);
            }
        } catch (Break breakEncounter) {
            System.err.println("BreakEncounter caught");
        }
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        System.err.println("Encountered a break. Throwing");
        throw new Break();
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        currentEnvironment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        TokenType logicalOperator = expr.operator.getType();
        if (logicalOperator == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else if (logicalOperator == TokenType.AND) {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object rhs = evaluate(expr.right);

        Token operatorToken = expr.operator;
        switch (operatorToken.getType()) {
            case BANG: return !isTruthy(rhs);
            case MINUS:
                checkNumberOperand(operatorToken, rhs);
                return -(double) rhs;
            default: throw new LoxRuntimeError(operatorToken, "Unexpected token");
        }
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        Token variableName = expr.name;
        Object value = currentEnvironment.get(variableName);
        if (value == VariableUninitialized.UNINITIALIZED) {
            throw new LoxRuntimeError(variableName, "Uninitialized variable ");
        }
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object lhs = evaluate(expr.left);
        Object rhs = evaluate(expr.right);

        Token operatorToken = expr.operator;
        switch (operatorToken.getType()) {
            case BANG_EQUAL:
                return !isEqual(lhs, rhs);
            case EQUAL_EQUAL:
                return isEqual(lhs, rhs);
            case GREATER:
                checkNumberOperands(operatorToken, lhs, rhs);
                return (double) lhs > (double) rhs;
            case GREATER_EQUAL:
                checkNumberOperands(operatorToken, lhs, rhs);
                return (double) lhs >= (double) rhs;
            case LESS:
                checkNumberOperands(operatorToken, lhs, rhs);
                return (double) lhs < (double) rhs;
            case LESS_EQUAL:
                checkNumberOperands(operatorToken, lhs, rhs);
                return (double) lhs <= (double) rhs;
            case MINUS:
                checkNumberOperands(operatorToken, lhs, rhs);
                return (double) lhs - (double) rhs;
            case PLUS:
                if (lhs instanceof Double && rhs instanceof Double) {
                    return (double) lhs + (double) rhs;
                } else if (lhs instanceof String || rhs instanceof String) {
                    return stringify(lhs) + stringify(rhs);
                } else {
                    throw new LoxRuntimeError(operatorToken, "Only Strings and Numbers are supported");
                }
            case STAR:
                checkNumberOperands(operatorToken, lhs, rhs);
                return (double) lhs * (double) rhs;
            case SLASH:
                checkNumberOperands(operatorToken, lhs, rhs);
                double evalResult = (double) lhs / (double) rhs;
                if (Double.isInfinite(evalResult)) {
                    throw new LoxRuntimeError(operatorToken, "Division by zero");
                }
                return evalResult;
            default: throw new LoxRuntimeError(operatorToken, "Unexpected token");
        }
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        // note that argument expressions are evaluated in order of their appearance
        List<Object> arguments = expr.arguments.stream()
                .map(this::evaluate)
                .collect(Collectors.toList());

        if (!(callee instanceof LoxCallable)) {
            throw new LoxRuntimeError(expr.paren, "Can only call functions and classes");
        }

        LoxCallable function = (LoxCallable) callee;

        if (arguments.size() != function.arity()) {
            throw new LoxRuntimeError(expr.paren, String.format(
                    "Expected %s arguments but got %s.",
                    function.arity(),
                    arguments.size()
            ));
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitConditionalExpr(Expr.Conditional expr) {
        Object evalCondition = evaluate(expr.condition);
        return isTruthy(evalCondition) ? evaluate(expr.caseTrue) : evaluate(expr.caseFalse);
    }

    private void checkNumberOperands(Token operator, Object lhs, Object rhs) {
        if (lhs instanceof Double && rhs instanceof Double) return;
        throw new LoxRuntimeError(operator, "Operands must be numbers.");
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new LoxRuntimeError(operator, "Operand must be a number.");
    }

    /**
     * Everything except {@code nil} and {@code false} is {@code truthy}
     * Like in Ruby
     */
    private boolean isTruthy(Object object) {
        if (Objects.isNull(object)) {
            return false;
        }

        if (object instanceof Boolean) {
            return (boolean) object;
        }

        return true;
    }

    private boolean isEqual(Object a, Object b) {
        // nil is only equal to nil.
        return Objects.equals(a, b);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.currentEnvironment;
        try {
            this.currentEnvironment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.currentEnvironment = previous;
        }
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        // Hack. Work around Java adding ".0" to integer-valued doubles.
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    /**
     * Used to mark variables as defined but not initialized and throw error in case such variable is used in a
     * Statement.
     */
    enum VariableUninitialized {
        UNINITIALIZED
    }

    private static class Break extends RuntimeException {
        Break() {
            super(null, null, false, false);
        }
    }

}
