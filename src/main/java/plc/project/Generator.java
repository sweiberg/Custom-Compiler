package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        indent++;
        newline(0);

        if (!ast.getFields().isEmpty()) {
            for (Ast.Field field : ast.getFields()) {
                newline(indent);
                print(field);
            }

            newline(0);
        }

        newline(indent);
        print("public static void main(String[] args) {");
        indent++;

        newline(indent);
        print("System.exit(new Main().main());");
        indent--;

        newline(indent);
        print("}");
        newline(0);

        for (Ast.Method method : ast.getMethods()) {
            newline(indent);
            print(method);
        }

        newline(0);
        indent--;

        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        switch (ast.getTypeName()) {
            case "Integer":
                print("int");
                break;
            case "Decimal":
                print("double");
                break;
            case "Boolean":
                print("boolean");
                break;
            case "Character":
                print("char");
                break;
            case "String":
                print("String");
                break;
            default:
                break;
        }

        print(" ");
        print(ast.getName());

        if (ast.getValue().isPresent()) {
            print(" = ");
            print(ast.getValue().get());
        }

        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        print(ast.getFunction().getReturnType().getJvmName());
        print(" ");

        print(ast.getName());
        print("(");

        int parameterSize = ast.getParameters().size();
        int lastIndex = parameterSize - 1;

        for (int i = 0; i < parameterSize; i++) {
            print(ast.getParameterTypeNames().get(i));
            print(" ");
            print(ast.getParameters().get(i));

            if (i != lastIndex) {
                print(", ");
            }
        }

        print(") {");

        if (!ast.getStatements().isEmpty()) {
            indent++;

            for (Ast.Stmt statement : ast.getStatements()) {
                newline(indent);
                print(statement);
            }

            indent--;
            newline(indent);
        }

        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        print(ast.getExpression());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        print(ast.getVariable().getType().getJvmName());
        print(" ");
        print(ast.getVariable().getJvmName());

        if (ast.getValue().isPresent()) {
            print(" = ");
            print(ast.getValue().get());
        }

        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        print(ast.getReceiver());
        print(" = ");

        print(ast.getValue());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        print("if (");
        print(ast.getCondition());
        
        print(") {");
        indent++;

        for (int i = 0; i < ast.getThenStatements().size(); i++) {
            newline(indent);
            print(ast.getThenStatements().get(i));
        }

        indent--;
        newline(indent);
        print("}");

        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            indent++;

            for (Ast.Stmt elseStatement : ast.getElseStatements()) {
                newline(indent);
                print(elseStatement);
            }

            indent--;
            newline(indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        throw new UnsupportedOperationException(); //TODO
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        throw new UnsupportedOperationException(); //TODO
        return null;
    }

}
