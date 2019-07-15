package com.craftinginterpreters.lox;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RpnPrinterTest {

    @ParameterizedTest
    @MethodSource
    void rpnPrinterTest(Expr expr, String expectedValue) {
        RpnPrinter rpnPrinter = new RpnPrinter();
        assertThat(rpnPrinter.print(expr), is(expectedValue));
    }

    static Stream<Arguments> rpnPrinterTest() {
        return Stream.of(
                Arguments.arguments(
                        new Expr.Literal(1),
                        "1"
                ),
                Arguments.arguments(
                        new Expr.Unary(
                                new Token(TokenType.MINUS, "-", null, 1),
                                new Expr.Literal(1)
                        ),
                        "1 -"
                ),
                Arguments.arguments(
                        new Expr.Binary(
                                new Expr.Literal(1),
                                new Token(TokenType.SLASH, "/", null, 1),
                                new Expr.Literal(2)
                        ),
                        "1 2 /"
                ),
                Arguments.arguments(
                        new Expr.Binary(
                                new Expr.Grouping(
                                        new Expr.Binary(
                                                new Expr.Literal(1),
                                                new Token(TokenType.PLUS, "+", null, 1),
                                                new Expr.Literal(2)
                                        )
                                ),
                                new Token(TokenType.STAR, "*", null, 1),
                                new Expr.Grouping(
                                        new Expr.Binary(
                                                new Expr.Literal(4),
                                                new Token(TokenType.MINUS, "-", null, 1),
                                                new Expr.Literal(3)
                                        )
                                )
                        ),
                        "1 2 + 4 3 - *"
                ),
                Arguments.arguments(
                        new Expr.Binary(
                                new Expr.Unary(
                                        new Token(TokenType.MINUS, "-", null, 1),
                                        new Expr.Literal(123)
                                ),
                                new Token(TokenType.STAR, "*", null, 1),
                                new Expr.Grouping(
                                        new Expr.Literal(45.67)
                                )
                        ),
                        "123 - 45.67 *"
                ),
                Arguments.arguments(
                        new Expr.Ternary(
                                new Expr.Literal(true),
                                new Expr.Literal(1),
                                new Expr.Ternary(
                                        new Expr.Literal(true),
                                        new Expr.Literal(2),
                                        new Expr.Literal(3)
                                )
                        ),
                        "1 2 3 true ? true ?"
                ),
                Arguments.arguments(
                        new Expr.Ternary(
                                new Expr.Literal(true),
                                new Expr.Ternary(
                                        new Expr.Literal(true),
                                        new Expr.Literal(1),
                                        new Expr.Literal(2)
                                ),
                                new Expr.Ternary(
                                        new Expr.Literal(true),
                                        new Expr.Literal(3),
                                        new Expr.Literal(4)
                                )
                        ),
                        "1 2 true ? 3 4 true ? true ?"
                )
        );
    }
}
