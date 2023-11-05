package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        List<Ast.Field> fields = ast.getFields();
        List<Ast.Method> methods = ast.getMethods();

        fields.forEach(this::visit);
        methods.forEach(this::visit);

        Environment.Function function = scope.lookupFunction("main", 0);

        return function.invoke(new ArrayList<>());
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        List<String> parameterList = ast.getParameters();

        scope.defineFunction(ast.getName(), parameterList.size(), args -> {
            Scope newscope = new Scope(scope);
            try {
                for (int i = 0; i < parameterList.size(); i++) {
                    newscope.defineVariable(parameterList.get(i), args.get(i));
                }

                scope = newscope;
                ast.getStatements().forEach(this::visit);

                return Environment.NIL;
            } catch (Return r) {
                return r.value;
            }
        });

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        if (ast.getReceiver().getClass() == Ast.Expr.Access.class) {
            scope = new Scope(scope);
            Ast.Expr.Access temp = (Ast.Expr.Access) ast.getReceiver();

            if (temp.getReceiver().isPresent()) {
                Environment.PlcObject receiver = visit(temp.getReceiver().get());
                receiver.setField(temp.getName(), visit(ast.getValue()));
            } else {
                scope.lookupVariable(temp.getName()).setValue(visit(ast.getValue()));
            }
        } else {
            throw new RuntimeException("Error: Assign Type");
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        boolean condition = requireType(Boolean.class, visit(ast.getCondition()));

        scope = new Scope(scope);

        if (condition) {
            ast.getThenStatements().forEach(this::visit);
        } else {
            ast.getElseStatements().forEach(this::visit);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject rec = visit(ast.getReceiver().get());
            return rec.getField(ast.getName()).getValue();
        }
        return scope.lookupVariable(ast.getName()).getValue();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        scope = new Scope(scope);
        List<Environment.PlcObject> args = new ArrayList<>();
        for (int i = 0; i < ast.getArguments().size(); i++) {
            args.add(visit(ast.getArguments().get(i)));
        }
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            return receiver.callMethod(ast.getName(), args);
        }
        else {
            return scope.lookupFunction(ast.getName(), args.size()).invoke(args);
        }
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
