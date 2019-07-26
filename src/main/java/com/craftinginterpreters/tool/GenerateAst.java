package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    // run with src/main/java/com/craftinginterpreters/lox as argument
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(1);
        }

        Path outputDirPath = Path.of(args[0]);
        if (!outputDirPath.toFile().isDirectory()) {
            System.err.printf("Received absolute path %s", outputDirPath.toAbsolutePath());
            System.err.printf("Path %s is not a directory", outputDirPath);
            System.exit(1);
        }

        defineAst(outputDirPath, "Expr", Arrays.asList(
                "Binary   : Expr left, Token operator, Expr right",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Unary    : Token operator, Expr right",
                "Ternary  : Expr condition, Expr caseTrue, Expr caseFalse"
        ));

        defineAst(outputDirPath, "Stmt", Arrays.asList(
                "Expression : Expr expression",
                "Print      : Expr expression"
        ));
    }

    private static void defineAst(Path outputDirPath, String baseName, List<String> types) throws IOException {
        Path classFilePath = outputDirPath.resolve(baseName + ".java");
        try (PrintWriter writer = new PrintWriter(
                Files.newOutputStream(classFilePath), false, StandardCharsets.UTF_8)) {

            writer.println("package com.craftinginterpreters.lox;");
            writer.println();
            writer.println("import java.util.List;");
            writer.println("import lombok.EqualsAndHashCode;");
            writer.println();
            writer.println("/**");
            writer.println(" * This file is generated automatically by the GenerateAst.java");
            writer.println(" */");
            writer.printf("abstract class %s {\n", baseName);

            defineVisitor(writer, baseName, types);

            // The AST classes.
            for (String type : types) {
                String className = type.split(":")[0].trim();
                String fields = type.split(":")[1].trim();
                defineType(writer, baseName, className, fields);
            }

            // The base accept() method.
            writer.println();
            writer.println("  abstract <R> R accept(Visitor<R> visitor);");

            writer.println("}");
        }
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("  interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.printf("    R visit%s%s(%s %s);\n", typeName, baseName, typeName, baseName.toLowerCase());
        }

        writer.println("  }");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        // base Expr class isn't supposed to have a meaningful equals() for now
        writer.println("  @EqualsAndHashCode(callSuper = false)");
        writer.printf("  static class %s extends %s {\n", className, baseName );

        // Constructor.
        writer.printf("    %s(%s) {\n", className, fieldList);

        // Store parameters in fields.
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.printf("      this.%s = %s;\n", name, name);
        }

        writer.println("    }");

        // Visitor pattern.
        writer.println();
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.printf("      return visitor.visit%s%s(this);\n", className, baseName );
        writer.println("    }");

        // Fields.
        writer.println();
        for (String field : fields) {
            writer.printf("    final %s;\n", field);
        }

        writer.println("  }");
    }
}
