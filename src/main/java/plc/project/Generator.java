package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

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

    public void read(List<Ast.Stmt> statements) {
        newline(++indent);
        for (int i = 0; i < statements.size(); i++) {
            if (i != 0) {
                newline(indent);
            }
            print(statements.get(i));
        }
        newline(--indent);
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(indent);

        if (!ast.getFields().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getFields().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getFields().get(i));
            }
            newline(--indent);
        }

        newline(++indent);
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");
        newline(--indent);

        if (!ast.getMethods().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getMethods().size(); i++) {
                if (i != 0) {
                    newline(--indent);
                    newline(++indent);
                }
                print(ast.getMethods().get(i));
            }
            newline(--indent);
        }

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
        print(ast.getFunction().getReturnType().getJvmName(), " ", ast.getFunction().getJvmName(), "(");
        int parameterSize = ast.getParameters().size();

        for (int i = 0; i < parameterSize; i++) {
            if (i != 0) {
                print(", ");
            }
            print(ast.getFunction().getParameterTypes().get(i).getJvmName(), " " , ast.getParameters().get(i));
        }

        print(") {");

        if (!ast.getStatements().isEmpty()) {
            read(ast.getStatements());
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
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());

        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }

        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue(), ";");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        print("if (", ast.getCondition(), ") {");

        if (!ast.getThenStatements().isEmpty()) {
            read(ast.getThenStatements());
        }

        print("}");

        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            read(ast.getElseStatements());
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        print("for (", "int ", ast.getName(), " : ", ast.getValue(), ") {");

        if (!ast.getStatements().isEmpty()) {
            read(ast.getStatements());
        }

        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        print("while (", ast.getCondition(), ") {");

        if (!ast.getStatements().isEmpty()) {
            read(ast.getStatements());
        }

        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        print("return ");
        print(ast.getValue());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        if (ast.getType().equals(Environment.Type.CHARACTER)) {
            print("'");
            print(ast.getLiteral());
            print("'");
        } else if (ast.getType().equals(Environment.Type.STRING)) {
            print("\"");
            print(ast.getLiteral());
            print("\"");
        } else if (ast.getType().equals(Environment.Type.DECIMAL)) {
            if (ast.getLiteral() instanceof BigDecimal) {
                BigDecimal decimalLiteral = (BigDecimal) ast.getLiteral();
                print(decimalLiteral.doubleValue());
            }
        } else if (ast.getType().equals(Environment.Type.INTEGER)) {
            if (ast.getLiteral() instanceof BigInteger) {
                BigInteger integerLiteral = (BigInteger) ast.getLiteral();
                print(integerLiteral.intValue());
            }
        } else {
            print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(");
        print(ast.getExpression());
        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        print(ast.getLeft());
        print(" ");

        if (ast.getOperator().equals("AND")) {
            print("&&");
        }
        else if (ast.getOperator().equals("OR")) {
            print("||");
        }
        else {
            print(ast.getOperator());
        }

        print(" ");
        print(ast.getRight());

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            print(ast.getReceiver().get());
            print(".");
        }

        print(ast.getVariable().getJvmName());

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        if (ast.getReceiver().isPresent()) {
            print(ast.getReceiver().get());
            print(".");
        }

        print(ast.getFunction().getJvmName(), "(");

        for (int i = 0; i < ast.getArguments().size(); i++) {
            if (i != 0) {
                print(", ");
            }
            print(ast.getArguments().get(i));
        }

        print(")");

        return null;
    }

}
