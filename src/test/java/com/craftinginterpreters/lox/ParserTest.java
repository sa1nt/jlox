package com.craftinginterpreters.lox;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.craftinginterpreters.lox.TokenType.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ParserTest {
    @ParameterizedTest
    @MethodSource("parserTest")
    void parserTest(String source, Stmt expectedStmt) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);

        List<Stmt> statements = parser.parse();
        assertThat(statements.size(), is(1));

        Stmt stmt = statements.get(0);

        assertThat(stmt, is(expectedStmt));
    }

    static Stream<Arguments> parserTest() {
        return Stream.of(
                Arguments.arguments(
                        "1;",
                        new Stmt.Expression(
                                new Expr.Literal(1d)
                        )
                ),
                Arguments.arguments(
                        "(1 / 2);",
                        new Stmt.Expression(
                                new Expr.Grouping(
                                        new Expr.Binary(
                                                new Expr.Literal(1d),
                                                new Token(TokenType.SLASH, "/", null, 1),
                                                new Expr.Literal(2d)
                                        )
                                )
                        )
                ),
                Arguments.arguments(
                        "-123 * (45.67);",
                        new Stmt.Expression(
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
                        )
                ),
                Arguments.arguments(
                        "true ? 1 : true ? 2 : 3;",
                        new Stmt.Expression(
                                new Expr.Conditional(
                                        new Expr.Literal(true),
                                        new Expr.Literal(1d),
                                        new Expr.Conditional(
                                                new Expr.Literal(true),
                                                new Expr.Literal(2d),
                                                new Expr.Literal(3d)
                                        )
                                )
                        )
                ),
                Arguments.arguments(
                        "true ? true ? 1 : 2 : true ? 3 : 4;",
                        new Stmt.Expression(
                                new Expr.Conditional(
                                        new Expr.Literal(true),
                                        new Expr.Conditional(
                                                new Expr.Literal(true),
                                                new Expr.Literal(1d),
                                                new Expr.Literal(2d)
                                        ),
                                        new Expr.Conditional(
                                                new Expr.Literal(true),
                                                new Expr.Literal(3d),
                                                new Expr.Literal(4d)
                                        )
                                )
                        )
                ),
                Arguments.arguments(
                        "a = 2;",
                        new Stmt.Expression(
                                new Expr.Assign(
                                        new Token(IDENTIFIER, "a", null, 1),
                                        new Expr.Literal(2d)
                                )
                        )
                ),
                Arguments.arguments(
                        "test();",
                        new Stmt.Expression(
                                new Expr.Call(
                                        new Expr.Variable(
                                                new Token(IDENTIFIER, "test", null, 1)
                                        ),
                                        new Token(RIGHT_PAREN, ")", null, 1),
                                        Collections.emptyList()
                                )
                        )
                ),
                Arguments.arguments(
                        "test(a);",
                        new Stmt.Expression(
                                new Expr.Call(
                                        new Expr.Variable(
                                                new Token(IDENTIFIER, "test", null, 1)
                                        ),
                                        new Token(RIGHT_PAREN, ")", null, 1),
                                        List.of(
                                                new Expr.Variable(
                                                        new Token(IDENTIFIER, "a", null, 1)
                                                )
                                        )
                                )
                        )
                ),
                Arguments.arguments(
                        "test(1, b, test2());",
                        new Stmt.Expression(
                                new Expr.Call(
                                        new Expr.Variable(
                                                new Token(IDENTIFIER, "test", null, 1)
                                        ),
                                        new Token(RIGHT_PAREN, ")", null, 1),
                                        Arrays.asList(
                                                new Expr.Literal(1d),
                                                new Expr.Variable(
                                                        new Token(IDENTIFIER, "b", null, 1)
                                                ),
                                                new Expr.Call(
                                                        new Expr.Variable(
                                                                new Token(IDENTIFIER, "test2", null, 1)
                                                        ),
                                                        new Token(RIGHT_PAREN, ")", null, 1),
                                                        Collections.emptyList()
                                                )
                                        )
                                )
                        )
                ),
                Arguments.arguments(
                        "a,test(),3;",
                        new Stmt.Expression(
                                new Expr.Binary(
                                        new Expr.Binary(
                                                new Expr.Variable(
                                                        new Token(IDENTIFIER, "a", null, 1)
                                                ),
                                                new Token(COMMA, ",", null, 1),
                                                new Expr.Call(
                                                        new Expr.Variable(
                                                                new Token(IDENTIFIER, "test", null, 1)
                                                        ),
                                                        new Token(RIGHT_PAREN, ")", null, 1),
                                                        Collections.emptyList()
                                                )
                                        ),
                                        new Token(COMMA, ",", null, 1),
                                        new Expr.Literal(3d)
                                )
                        )
                ),
                Arguments.arguments(
                        "fun test(a) { print a; return a + 1; }",
                        new Stmt.Function(
                                new Token(IDENTIFIER, "test", null, 1),
                                List.of(new Token(IDENTIFIER, "a", null, 1)),
                                List.of(
                                        new Stmt.Print(
                                                new Expr.Variable(new Token(
                                                        IDENTIFIER, "a", null, 1
                                                ))
                                        ),
                                        new Stmt.Return(
                                                new Token(RETURN, "return", null, 1),
                                                new Expr.Binary(
                                                        new Expr.Variable(new Token(
                                                                IDENTIFIER, "a", null, 1
                                                        )),
                                                        new Token(PLUS, "+", null, 1),
                                                        new Expr.Literal(1d)
                                                )
                                        )
                                )
                        )
                )
        );
    }
}
