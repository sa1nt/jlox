package com.craftinginterpreters.lox;

import java.util.List;
import lombok.EqualsAndHashCode;

/**
 * This file is generated automatically by the GenerateAst.java
 */
abstract class Stmt {
  interface Visitor<R> {
    R visitExpressionStmt(Expression stmt);
    R visitPrintStmt(Print stmt);
  }
  @EqualsAndHashCode(callSuper = false)
  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }

    final Expr expression;
  }
  @EqualsAndHashCode(callSuper = false)
  static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }

    final Expr expression;
  }

  abstract <R> R accept(Visitor<R> visitor);
}
