package com.craftinginterpreters.lox;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;
import static org.hamcrest.MatcherAssert.assertThat;

class ScannerTest {
    @Test
    void test_scanTokens() {
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
}
