package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.Stmt.Function;
import com.craftinginterpreters.lox.Stmt.Return;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * program     → declaration* EOF ;
 *
 * declaration → funDecl
 *             | varDeclaration
 *             | statement ;
 *
 * funDecl   → "fun" function ;
 *
 * function  → IDENTIFIER "(" parameters? ")" block;
 *
 * parameters → IDENTIFIER ( "," IDENTIFIER )* ;
 *
 * statement → exprStmt
 *           | ifStmt
 *           | forStmt
 *           | returnStmt
 *           | printStmt
 *           | whileStmt
 *           | breakStmt
 *           | block ;
 *
 * ifStmt    → "if" "(" expression ")" statement ( "else" statement )? ;
 *
 * forStmt   → "for" "(" ( varDecl | exprStmt | ";" )
 *                       expression? ";"
 *                       expression? ")" statement ;
 *
 * block     → "{" declaration* "}" ;
 *
 * returnStmt → "return" expression? ";" ;
 *
 * exprStmt  → expression ";" ;
 * printStmt → "print" expression ";" ;
 * whileStmt → "while" "(" expression ")" statement ;
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
 * unary              → ( "!" | "-" ) unary
 *                    | call
 *                    | primary
 *                    // Error productions for cases when a binary operation is missing a left operand
 *                    | ( "+" | "/"  | "*" ) unary ;
 * call               → primary ( "(" arguments? ")" )* ;
 *
 * // we're using 'assignment' instead of 'expression' here because
 * //   'expression' resolves in 'comma', and that's not what we want
 * arguments          → assignment ( "," assignment )* ;
 * primary            → "true" | "false" | "nil"
 *                    | NUMBER | STRING
 *                    | "(" expression ")"
 *                    | IDENTIFIER ;
 */
class Parser {
    private final List<Token> tokens;
    private boolean expectBreak = false;

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
            if (match(FUN)) return function("function");
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
     *           | forStmt
     *           | printStmt
     *           | whileStmt
     *           | returnStmt
     *           | breakStmt
     *           | block ;
     */
    private Stmt statement() {
        if (match(FOR)) return finishForStatement();
        if (match(IF)) return finishIfStatement();
        if (match(PRINT)) return finishPrintStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return finishWhileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(finishBlockStatement());

        if (match(BREAK)) {
            if (expectBreak) {
                consume(SEMICOLON, "Expected ';' after break");
                return new Stmt.Break();
            } else {
                //noinspection ThrowableNotThrown
                error(previous(), "break; statement allowed only inside a loop");
            }
        }

        return finishExpressionStatement();
    }

    /**
     * forStmt   → "for" "(" ( varDecl | exprStmt | ";" )
     *                       expression? ";"
     *                       expression? ")" statement ;
     *
     * The idea is to desugar a for loop Statement into a while loop
     */
    private Stmt finishForStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = finishVarDeclaration();
        } else {
            initializer = finishExpressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        // expectBreak should be false if this loop has no loops wrapping it
        boolean outerLoop = !this.expectBreak;
        if (outerLoop) this.expectBreak = true;

        Stmt body = statement();

        // stop expecting break; statements when not in a loop
        if (outerLoop) {
            this.expectBreak = false;
        }

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(
                    body,
                    new Stmt.Expression(increment)
            ));
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
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
     * returnStmt → "return" expression? ";" ;
     */
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value");
        return new Return(keyword, value);
    }

    /**
     * whileStmt → "while" "(" expression ")" statement ;
     */
    private Stmt finishWhileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");

        // expectBreak should be false if this loop has no loops wrapping it
        boolean outerLoop = !this.expectBreak;
        if (outerLoop) this.expectBreak = true;

        Stmt body = statement();

        // stop expecting break; statements when not in a loop
        if (outerLoop) {
            this.expectBreak = false;
        }
        return new Stmt.While(condition, body);
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
     * declaration -> funDecl
     *              | var
     */
    private Function function(String kind) {
        Token name = consume(IDENTIFIER, String.format("Expect %s name.", kind));
        consume(LEFT_PAREN, String.format("Expect '(' after %s name.", kind));
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    //noinspection ThrowableNotThrown
                    error(peek(), "Cannot have more than 255 parameters");
                }

                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters");

        consume(LEFT_BRACE, String.format("Expect '{' before %s body", kind));
        List<Stmt> body = finishBlockStatement();
        return new Function(name, parameters, body);
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
     * conditionalExpr → logic_or ("?" conditionalExpr ":" conditionalExpr)* ;
     * Has low precedence and is right-associative
     * As per https://en.wikipedia.org/wiki/%3F:
     */
    private Expr conditionalExpr() {
        Expr condition = logic_or();
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
     * logic_or           → logic_and ( "or" logic_and )* ;
     */
    private Expr logic_or() {
        Expr expr = logic_and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = logic_and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /**
     * logic_and          → equality ( "and" equality )* ;
     */
    private Expr logic_and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
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
     * unary              → ( "!" | "-" ) unary
     *                    | call
     *                    | primary
     *                    // Error productions for cases when a binary operation is missing a left operand
     *                    | ( "+" | "/"  | "*" ) unary ;
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

        return call();
    }

    /*
     * call               → primary ( "(" arguments? ")" )* ;
     */
    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    //noinspection ThrowableNotThrown
                    error(peek(), "Cannot have more than 255 arguments");
                }
                /*
                 * we're using 'assignment' instead of 'expression' here because
                 *   'expression' resolves in 'comma', and that's not what we want
                 */
                arguments.add(assignment());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments");

        return new Expr.Call(callee, paren, arguments);
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
