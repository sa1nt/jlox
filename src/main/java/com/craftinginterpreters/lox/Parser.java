package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * program     → declaration* EOF ;
 *
 * declaration → varDeclaration
 *             | statement ;
 *
 * statement → exprStmt
 *           | ifStmt
 *           | printStmt
 *           | block ;
 *
 * ifStmt    → "if" "(" expression ")" statement ( "else" statement )? ;
 *
 * block     → "{" declaration* "}" ;
 *
 * exprStmt  → expression ";" ;
 * printStmt → "print" expression ";" ;
 * varDeclaration → "var" IDENTIFIER ( "=" expression )? ";" ;
 *
 * See also https://en.cppreference.com/w/c/language/operator_precedence for
 * inspiration
 *
 * expression         → comma ;
 * comma              → assignment ( "," assignment )* ;
 * assignment         → IDENTIFIER "=" assignment
 *                    | conditionalExpr;
 * conditionalExpr    → logic_or ("?" conditionalExpr ":" conditionalExpr)* ;
 * logic_or           → logic_and ( "or" logic_and )* ;
 * logic_and          → equality ( "and" equality )* ;
 * equality           → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison         → addition ( ( ">" | ">=" | "<" | "<=" ) addition )* ;
 * addition           → multiplication ( ( "-" | "+" ) multiplication )* ;
 * multiplication     → unary ( ( "/" | "*" ) unary )* ;
 * unary → ( "!" | "-" ) unary
 *       | primary
 *       // Error productions for cases when a binary operation is missing a left operand
 *       | ( "+" | "/"  | "*" ) unary ;
 * primary → "true" | "false" | "nil"
 *         | NUMBER | STRING
 *         | "(" expression ")"
 *         | IDENTIFIER ;
 */
class Parser {
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    /**
     * declaration → varDeclaration
     *             | statement ;
     */
    private Stmt declaration() {
        try {
            if (match(VAR)) return finishVarDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    /**
     * statement → exprStmt
     *           | ifStmt
     *           | printStmt
     *           | block ;
     */
    private Stmt statement() {
        if (match(IF)) return finishIfStatement();
        if (match(PRINT)) return finishPrintStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(finishBlockStatement());

        return finishExpressionStatement();
    }

    /**
     * ifStmt    → "if" "(" expression ")" statement ( "else" statement )? ;
     */
    private Stmt finishIfStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /**
     * printStmt → "print" expression ";" ;
     * Note that "print" Token is consumed at {@code statement()}
     */
    private Stmt finishPrintStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    /**
     * varDeclaration → "var" IDENTIFIER ( "=" expression )? ";" ;
     */
    private Stmt finishVarDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    /**
     * exprStmt  → expression ";" ;
     */
    private Stmt finishExpressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    /**
     * block     → "{" declaration* "}" ;
     */
    private List<Stmt> finishBlockStatement() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    /**
     * expression     → comma ;
     */
    private Expr expression() {
        return comma();
    }

    /**
     * comma → assignment ( "," assignment )* ;
     */
    private Expr comma() {
        Expr expr = assignment();
        expr = parseLeftAssociativeSeries(expr, List.of(COMMA), this::assignment);
        return expr;
    }

    /**
     * assignment     → IDENTIFIER "=" assignment
     *                | conditionalExpr;
     */
    private Expr assignment() {
        Expr expr = conditionalExpr();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            //noinspection ThrowableNotThrown
            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    /**
     * Conditional (ternary) operator
     * conditionalExpr → equality ("?" conditionalExpr ":" conditionalExpr)* ;
     * Has low precedence and is right-associative
     * As per https://en.wikipedia.org/wiki/%3F:
     */
    private Expr conditionalExpr() {
        Expr condition = equality();
        if (match(QUESTION)) {
            Expr caseTrue = conditionalExpr();
            if (match(COLON)) {
                Expr caseFalse = conditionalExpr();
                return new Expr.Conditional(condition, caseTrue, caseFalse);
            }
        }
        return condition;
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
     *       | primary
     *       // Error productions for cases when a binary operation is missing a left operand
     *       | ( "+" | "/"  | "*" ) unary ;
     */
    private Expr unary() {
        if (match(List.of(BANG, MINUS))) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        if (match(List.of(PLUS, SLASH, STAR))) {
            Token previous = previous();
            Lox.error(previous, "Not allowed as a unary operator");
        }

        return primary();
    }

    /**
     * primary → "true" | "false" | "nil"
     *         | NUMBER | STRING
     *         | "(" expression ")"
     *         | IDENTIFIER ;
     */
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(List.of(NUMBER, STRING))) {
            return new Expr.Literal(previous().getLiteral());
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
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

    /**
     * If current token matches given, consumes it and returns next token.
     * Throws otherwise.
     */
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

    /**
     * Advances if current token matches any of given tokens
     */
    private boolean match(Collection<TokenType> types) {
        return types.stream().anyMatch(this::match);
    }

    /**
     * Advances if current token matches given
     */
    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    /**
     * Checks if current token is same as given token
     */
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

    /**
     * @return current token
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * @return previous token
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    private static class ParseError extends RuntimeException {}
}
