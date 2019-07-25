package com.craftinginterpreters.lox;

import java.util.Objects;

public class Interpreter implements Expr.Visitor<Object> {

    // TODO: return result of stringify instead of printing right here?
    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (LoxRuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
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
    public Object visitTernaryExpr(Expr.Ternary expr) {
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
}
