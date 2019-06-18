package com.craftinginterpreters.lox;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class AstPrinterTest {

    @ParameterizedTest
    @MethodSource("astPrintTest")
    void astPrintTest(Expr expr, String expectedValue) {
        AstPrinter astPrinter = new AstPrinter();
        assertThat(astPrinter.print(expr), is(expectedValue));
    }

    static Stream<Arguments> astPrintTest() {
        return Stream.of(
                Arguments.arguments(
                        new Expr.Binary(
                                new Expr.Literal(1),
                                new Token(TokenType.SLASH, "/", null, 1),
                                new Expr.Literal(2)
                        ),
                        "(/ 1 2)"
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
                        "(* (- 123) (group 45.67))"
                )
        );
    }
}
