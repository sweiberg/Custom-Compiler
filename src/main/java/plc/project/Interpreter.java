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
        if (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        else if (!requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getElseStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        Iterable iter = requireType(Iterable.class, visit(ast.getValue()));

        for (Object obj : iter) {
            scope = new Scope(scope);
            scope.defineVariable(ast.getName(), (Environment.PlcObject) obj);
            ast.getStatements().forEach(this::visit);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        boolean condition;

        do {
            condition = requireType(Boolean.class, visit(ast.getCondition()));
            if (condition) {
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            }
        } while (condition);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        }
        else {
            return Environment.create(ast.getLiteral());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return Environment.create(visit(ast.getExpression()).getValue());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        String op = ast.getOperator();
        Object left, right;

        switch (op) {
            case "AND":
                if (requireType(Boolean.class, visit(ast.getLeft())) == requireType(Boolean.class, visit(ast.getRight()))) {
                    return visit(ast.getLeft());
                } else {
                    return Environment.create(Boolean.FALSE);
                }

            case "OR":
                if (requireType(Boolean.class, visit(ast.getLeft())) == Boolean.TRUE) {
                    return visit(ast.getLeft());
                } else if (requireType(Boolean.class, visit(ast.getRight())) == Boolean.TRUE) {
                    return visit(ast.getRight());
                } else {
                    return Environment.create(Boolean.FALSE);
                }

            case "<":
            case "<=":
            case ">":
            case ">=":
                left = visit(ast.getLeft()).getValue();
                right = visit(ast.getRight()).getValue();
                if (left instanceof Comparable && left.getClass() == right.getClass()) {
                    int compare = ((Comparable<Object>) left).compareTo(right);
                    switch (op) {
                        case "<":
                            return compare < 0 ? Environment.create(Boolean.TRUE) : Environment.create(Boolean.FALSE);
                        case "<=":
                            return compare <= 0 ? Environment.create(Boolean.TRUE) : Environment.create(Boolean.FALSE);
                        case ">":
                            return compare > 0 ? Environment.create(Boolean.TRUE) : Environment.create(Boolean.FALSE);
                        case ">=":
                            return compare >= 0 ? Environment.create(Boolean.TRUE) : Environment.create(Boolean.FALSE);
                    }
                }

            case "==":
                if (visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue())) {
                    return Environment.create(Boolean.TRUE);
                } else {
                    return Environment.create(Boolean.FALSE);
                }

            case "!=":
                if (visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue())) {
                    return Environment.create(Boolean.FALSE);
                } else {
                    return Environment.create(Boolean.TRUE);
                }

            case "+":
                left = visit(ast.getLeft()).getValue();
                right = visit(ast.getRight()).getValue();

                if (left instanceof String || right instanceof String) {
                    return Environment.create(left.toString() + right.toString());
                } else if (left instanceof BigInteger && right instanceof BigInteger) {
                    return Environment.create(((BigInteger) left).add((BigInteger) right));
                } else if (left instanceof BigDecimal && right instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) left).add((BigDecimal) right));
                } else {
                    throw new RuntimeException("Type Error: Addition");
                }

            case "-":
            case "*":
                left = visit(ast.getLeft()).getValue();
                right = visit(ast.getRight()).getValue();
                if ((left.getClass() == BigDecimal.class || left.getClass() == BigInteger.class) && left.getClass() == right.getClass()) {
                    if (left.getClass() == BigInteger.class) {
                        return Environment.create(
                                op.equals("*")
                                        ? BigInteger.class.cast(left).multiply(BigInteger.class.cast(right))
                                        : BigInteger.class.cast(left).subtract(BigInteger.class.cast(right))
                        );
                    } else {
                        return Environment.create(
                                op.equals("*")
                                        ? BigDecimal.class.cast(left).multiply(BigDecimal.class.cast(right))
                                        : BigDecimal.class.cast(left).subtract(BigDecimal.class.cast(right))
                        );
                    }
                } else {
                    throw new RuntimeException("Type Error: Multiply Subtract");
                }

            case "/":
                left = visit(ast.getLeft()).getValue();
                right = visit(ast.getRight()).getValue();
                if ((left.getClass() == BigDecimal.class || left.getClass() == BigInteger.class) && left.getClass() == right.getClass()) {
                    if (BigDecimal.ZERO.equals(right) || BigInteger.ZERO.equals(right)) {
                        throw new RuntimeException("Edge Case: You can't divide by zero.");
                    }
                    return (left instanceof BigDecimal)
                            ? Environment.create(BigDecimal.class.cast(left).divide(BigDecimal.class.cast(right), RoundingMode.HALF_EVEN))
                            : Environment.create(BigInteger.class.cast(left).divide(BigInteger.class.cast(right)));
                } else {
                    throw new RuntimeException("Type Error: Forward Bracket");
                }

            default:
                throw new RuntimeException("Type Error");
        }
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
        try {
            scope = new Scope(scope);
            List<Environment.PlcObject> args = new ArrayList<>();
            for (int i = 0; i < ast.getArguments().size(); i++) {
                args.add(visit(ast.getArguments().get(i)));
            }
            if (ast.getReceiver().isPresent()) {
                Environment.PlcObject receiver = visit(ast.getReceiver().get());
                return receiver.callMethod(ast.getName(), args);
            } else {
                return scope.lookupFunction(ast.getName(), args.size()).invoke(args);
            }
        } finally {
            scope = scope.getParent();
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
