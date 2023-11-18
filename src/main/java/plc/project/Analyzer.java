package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        try {
            boolean args = false;

            if (!ast.getMethods().isEmpty()) {
                for (int i = 0; i < ast.getMethods().size(); i++) {
                    visit(ast.getMethods().get(i));
                    Ast.Method main = ast.getMethods().get(i);
                    if (main.getName().equals("main") && main.getReturnTypeName().get().equals("Integer") && main.getParameters().isEmpty()) {
                        args = true;
                    }
                }
            }

            if (!args) {
                throw new RuntimeException("Error: Main");
            }
        } catch (RuntimeException r) {
            throw new RuntimeException(r);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
         try {
            if (ast.getValue().isPresent()) {
                visit(ast.getValue().get());
                requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
                scope.defineVariable(ast.getName(), ast.getName(), ast.getValue().get().getType(), Environment.NIL);
                ast.setVariable(scope.lookupVariable(ast.getName()));
            }
            else {
                scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), Environment.NIL);
                ast.setVariable(scope.lookupVariable(ast.getName()));
            }
        } catch (RuntimeException r) {
            throw new RuntimeException(r);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        try {
            Environment.Type returnType = ast.getReturnTypeName().isPresent() ? Environment.getType(ast.getReturnTypeName().get()) : Environment.Type.NIL;
            scope.defineVariable("returnType", "returnType", returnType, Environment.NIL);

            List<String> paramStrings = ast.getParameterTypeNames();
            Environment.Type[] paramTypes = paramStrings.isEmpty() ? new Environment.Type[0] : paramStrings.stream().map(Environment::getType).toArray(Environment.Type[]::new);

            scope.defineFunction(ast.getName(), ast.getName(), Arrays.asList(paramTypes), returnType, args -> Environment.NIL);

            if (!ast.getStatements().isEmpty()) {
                for (Ast.Stmt statement : ast.getStatements()) {
                    try {
                        scope = new Scope(scope);
                        visit(statement);
                    } finally {
                        scope = scope.getParent();
                    }
                }
            }
            ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));
        } catch (RuntimeException r) {
            throw new RuntimeException(r);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        throw new UnsupportedOperationException();  // TODO
    }

}
