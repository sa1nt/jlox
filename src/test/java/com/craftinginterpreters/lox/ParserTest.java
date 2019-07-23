package com.craftinginterpreters.lox;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
    @ParameterizedTest
    @MethodSource("parserTest")
    void parserTest(String source, Expr expectedExpr) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        Expr parsedExpression = parser.parse();
        assertThat(parsedExpression, is(expectedExpr));
    }

    static Stream<Arguments> parserTest() {
        return Stream.of(
                Arguments.arguments(
                        "1",
                        new Expr.Literal(1d)
                ),
                Arguments.arguments(
                        "(1 / 2)",
                        new Expr.Grouping(
                            new Expr.Binary(
                                    new Expr.Literal(1d),
                                    new Token(TokenType.SLASH, "/", null, 1),
                                    new Expr.Literal(2d)
                            )
                        )
                ),
                Arguments.arguments(
                        "-123 * (45.67)",
                        new Expr.Binary(
                                new Expr.Unary(
                                        new Token(TokenType.MINUS, "-", null, 1),
                                        new Expr.Literal(123d)
                                ),
                                new Token(TokenType.STAR, "*", null, 1),
                                new Expr.Grouping(
                                        new Expr.Literal(45.67)
                                )
                        )
                ),
                Arguments.arguments(
                        "true ? 1 : true ? 2 : 3",
                        new Expr.Ternary(
                                new Expr.Literal(true),
                                new Expr.Literal(1d),
                                new Expr.Ternary(
                                        new Expr.Literal(true),
                                        new Expr.Literal(2d),
                                        new Expr.Literal(3d)
                                )
                        )
                ),
                Arguments.arguments(
                        "true ? true ? 1 : 2 : true ? 3 : 4",
                        new Expr.Ternary(
                                new Expr.Literal(true),
                                new Expr.Ternary(
                                        new Expr.Literal(true),
                                        new Expr.Literal(1d),
                                        new Expr.Literal(2d)
                                ),
                                new Expr.Ternary(
                                        new Expr.Literal(true),
                                        new Expr.Literal(3d),
                                        new Expr.Literal(4d)
                                )
                        )
                )
        );
    }
}