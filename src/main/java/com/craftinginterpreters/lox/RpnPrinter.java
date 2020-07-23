package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.Expr.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Prints {@code Expr} in Reverse Polish Notation
 */
public class RpnPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Assign expr) {
        return expr.name.getLexeme() + " " + expr.value.accept(this) + " =" ;
    }

    @Override
    public String visitBinaryExpr(Binary expr) {
        return expr.left.accept(this) + " " + expr.right.accept(this) + " " + expr.operator.getLexeme();
    }

    @Override
    public String visitGroupingExpr(Grouping expr) {
        return expr.expression.accept(this);
    }

    @Override
    public String visitLiteralExpr(Literal expr) {
        return expr.value == null ? "nil" : expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Logical expr) {
        return expr.left.accept(this) + " " + expr.right.accept(this) + expr.operator.getLexeme();
    }

    @Override
    public String visitUnaryExpr(Unary expr) {
        return expr.right.accept(this) + " " + expr.operator.getLexeme();
    }

    @Override
    public String visitConditionalExpr(Conditional expr) {
        return expr.caseTrue.accept(this) + " " + expr.caseFalse.accept(this) + " " +
                expr.condition.accept(this) + " ?";
    }

    @Override
    public String visitVariableExpr(Variable expr) {
        return expr.name.getLexeme();
    }

    @Override
    public String visitCallExpr(Call expr) {
        List<Expr> reverseArgs = new ArrayList<>(expr.arguments);
        Collections.reverse(reverseArgs);

        return reverseArgs.stream()
                .map(argExpr -> argExpr.accept(this))
                .collect(Collectors.joining(",", "", expr.callee.accept(this)));
    }
}
