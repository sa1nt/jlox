package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class Lox {
    private static boolean hadParseError = false;
    private static boolean hadRuntimeError = false;
    private static final Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));

        List<Stmt> parseResult = parse(new String(bytes, Charset.defaultCharset()));
        if (hadParseError) System.exit(65);

        interpreter.interpret(parseResult);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            List<Stmt> parseResult = parse(reader.readLine());
            interpretInRepl(parseResult);
            hadParseError = false;
        }
    }

    private static List<Stmt> parse(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        Parser parser = new Parser(tokens);

        return parser.parse();
    }

    private static void interpretInRepl(List<Stmt> parseResult) {
        // Stop if there was a syntax error.
        if (hadParseError) return;

        printLastStatement(parseResult);
        interpreter.interpret(parseResult);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadParseError = true;
    }

    static void error(Token token, String message) {
        if (token.getType() == TokenType.EOF) {
            report(token.getLine(), " at end", message);
        } else {
            report(token.getLine(), " at '" + token.getLexeme() + "'", message);
        }
    }

    static void runtimeError(LoxRuntimeError error) {
        System.err.println(error.getMessage() + "[line " + error.token.getLine() + "]");
        hadRuntimeError = true;
    }

    private static void printLastStatement(List<Stmt> statements) {
        getLastInputStatement(statements)
                .filter(Stmt.Expression.class::isInstance)
                .map(Stmt.Expression.class::cast)
                .map(exprStmt -> new Stmt.Print(exprStmt.expression))
                .ifPresent(printStmt -> replaceLastStmt(statements, printStmt));
    }

    private static Optional<Stmt> getLastInputStatement(List<Stmt> statements) {
        return getLastElemIndex(statements).map(statements::get);
    }

    private static void replaceLastStmt(List<Stmt> statements, Stmt stmt) {
        getLastElemIndex(statements)
                .ifPresent(lastIdx -> statements.set(lastIdx, stmt));
    }

    private static Optional<Integer> getLastElemIndex(List<Stmt> statements) {
        return statements.isEmpty() ?
                Optional.empty() :
                Optional.of(statements.size() - 1);
    }
}
