package com.craftinginterpreters.lox;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.craftinginterpreters.lox.TokenType.*;
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
                                new Token(SLASH, "/", null, 1),
                                new Expr.Literal(2)
                        ),
                        "(/ 1 2)"
                ),
                Arguments.arguments(
                        new Expr.Binary(
                                new Expr.Unary(
                                        new Token(MINUS, "-", null, 1),
                                        new Expr.Literal(123)
                                ),
                                new Token(STAR, "*", null, 1),
                                new Expr.Grouping(
                                        new Expr.Literal(45.67)
                                )
                        ),
                        "(* (- 123) (group 45.67))"
                ),
                Arguments.arguments(
                        new Expr.Conditional(
                                new Expr.Literal(true),
                                new Expr.Literal(1),
                                new Expr.Conditional(
                                        new Expr.Literal(true),
                                        new Expr.Literal(2),
                                        new Expr.Literal(3)
                                )
                        ),
                        "(if true 1 (if true 2 3))"
                ),
                Arguments.arguments(
                        new Expr.Conditional(
                                new Expr.Literal(true),
                                new Expr.Conditional(
                                        new Expr.Literal(true),
                                        new Expr.Literal(1),
                                        new Expr.Literal(2)
                                ),
                                new Expr.Conditional(
                                        new Expr.Literal(true),
                                        new Expr.Literal(3),
                                        new Expr.Literal(4)
                                )
                        ),
                        "(if true (if true 1 2) (if true 3 4))"
                ),
                Arguments.arguments(
                        new Expr.Assign(
                                new Token(IDENTIFIER, "varName", null, 1),
                                new Expr.Literal(true)
                        ),
                        "(= varName true)"
                )
        );
    }
}
