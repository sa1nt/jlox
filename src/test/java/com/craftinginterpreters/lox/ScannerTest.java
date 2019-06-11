package com.craftinginterpreters.lox;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;
import static org.hamcrest.MatcherAssert.assertThat;

class ScannerTest {
    @Test
    void test_scanParens() {
        String source = "(())";
        Scanner scanner = new Scanner(source);

        List<Token> tokens = scanner.scanTokens();

        assertThat(tokens, Matchers.contains(
                new Token(LEFT_PAREN, "(", null, 1),
                new Token(LEFT_PAREN, "(", null, 1),
                new Token(RIGHT_PAREN, ")", null, 1),
                new Token(RIGHT_PAREN, ")", null, 1),
                new Token(EOF, "", null, 1)
        ));
    }

    @Test
    void test_scanPrintExpr() {
        String source = "print 123 + 456";
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        assertThat(tokens, Matchers.contains(
                new Token(PRINT, "print", null, 1),
                new Token(NUMBER, "123", 123.0, 1),
                new Token(PLUS, "+", null, 1),
                new Token(NUMBER, "456", 456.0, 1),
                new Token(EOF, "", null, 1)
        ));
    }

    @Test
    void test_scanLineComments() {
        String source = "print 123 / 456 \n " +
                "// this is a comment \n " +
                "print (1 - 2 * 3 / 4)";
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        assertThat(tokens, Matchers.contains(
                new Token(PRINT, "print", null, 1),
                new Token(NUMBER, "123", 123.0, 1),
                new Token(SLASH, "/", null, 1),
                new Token(NUMBER, "456", 456.0, 1),

                new Token(PRINT, "print", null, 3),
                new Token(LEFT_PAREN, "(", null, 3),
                new Token(NUMBER, "1", 1.0, 3),
                new Token(MINUS, "-", null, 3),
                new Token(NUMBER, "2", 2.0, 3),
                new Token(STAR, "*", null, 3),
                new Token(NUMBER, "3", 3.0, 3),
                new Token(SLASH, "/", null, 3),
                new Token(NUMBER, "4", 4.0, 3),
                new Token(RIGHT_PAREN, ")", null, 3),
                new Token(EOF, "", null, 3)
        ));
    }

    @Test
    void test_scanMultilineComments() {
        String source = "print; \n " +
                "/* \n " +
                "comment \n " +
                "*/ \n " +
                "class";
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        assertThat(tokens, Matchers.contains(
                new Token(PRINT, "print", null, 1),
                new Token(SEMICOLON, ";", null, 1),

                new Token(CLASS, "class", null, 5),
                new Token(EOF, "", null, 5)
        ));
    }
}
