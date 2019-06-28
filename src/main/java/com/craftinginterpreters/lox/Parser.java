package com.craftinginterpreters.lox;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression() {
        return equality();
    }

    /**
     * equality → comparison ( ( "!=" | "==" ) comparison )* ;
     */
    private Expr equality() {
        Expr expr = comparison();
        expr = parseLeftAssociativeSeries(expr, List.of(BANG_EQUAL, EQUAL_EQUAL), this::comparison);
        return expr;
    }

    /**
     * comparison → addition ( ( ">" | ">=" | "<" | "<=" ) addition )* ;
     */
    private Expr comparison() {
        Expr expr = addition();
        expr = parseLeftAssociativeSeries(expr, List.of(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL), this::addition);
        return expr;
    }

    /**
     * addition → multiplication ( ( "+" | "-" ) multiplication )* ;
     */
    private Expr addition() {
        Expr expr = multiplication();
        expr = parseLeftAssociativeSeries(expr, List.of(MINUS, PLUS), this::multiplication);
        return expr;
    }

    /**
     * multiplication → unary ( ( "/" | "*" ) unary )* ;
     */
    private Expr multiplication() {
        Expr expr = unary();
        expr = parseLeftAssociativeSeries(expr, List.of(SLASH, STAR), this::unary);
        return expr;
    }

    /**
     * unary → ( "!" | "-" ) unary
     *       | primary ;
     */
    private Expr unary() {
        if (match(List.of(BANG, MINUS))) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    /**
     * primary → NUMBER | STRING | "false" | "true" | "nil"
     *         | "(" expression ")" ;
     */
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(List.of(NUMBER, STRING))) {
            return new Expr.Literal(previous().getLiteral());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private Expr parseLeftAssociativeSeries(Expr lhs, Collection<TokenType> tokenTypes, Supplier<Expr> rightSupplier) {
        while (match(tokenTypes)) {
            Token operator = previous();
            Expr right = rightSupplier.get();
            lhs = new Expr.Binary(lhs, operator, right);
        }
        return lhs;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    // TODO: will be used later for error handling
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().getType() == SEMICOLON) return;

            switch (peek().getType()) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }

    private boolean match(Collection<TokenType> types) {
        return types.stream().anyMatch(this::match);
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().getType() == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private static class ParseError extends RuntimeException {}
}
