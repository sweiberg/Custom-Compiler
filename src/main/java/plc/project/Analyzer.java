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
        visit(ast.getExpression());

        try {
            if (ast.getExpression().getClass() != Ast.Expr.Function.class) {
                throw new RuntimeException("Error: No Function Type");
            }
        }
        catch (RuntimeException r) {
            throw new RuntimeException(r);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        try {
            if (ast.getValue().isPresent()) {
                visit(ast.getValue().get());
                scope.defineVariable(ast.getName(), ast.getName(), ast.getValue().get().getType(), Environment.NIL);
                ast.setVariable(scope.lookupVariable(ast.getName()));
            }
            else {
                scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName().get()), Environment.NIL);
                ast.setVariable(scope.lookupVariable(ast.getName()));
            }
        } catch (RuntimeException r) {
            throw new RuntimeException(r);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        try {
            if (ast.getReceiver().getClass() != Ast.Expr.Access.class) {
                throw new RuntimeException("Error: No access");
            }

            visit(ast.getValue());
            visit(ast.getReceiver());
            requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        } catch (RuntimeException r) {
            throw new RuntimeException(r);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        try {
            if (ast.getThenStatements().isEmpty()) throw new RuntimeException("Error: Missing statement");

            visit(ast.getCondition());
            requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

            List<List<Ast.Stmt>> statementLists = Arrays.asList(ast.getElseStatements(), ast.getThenStatements());

            for (List<Ast.Stmt> statements : statementLists) {
                for (Ast.Stmt statement : statements) {
                    try {
                        scope = new Scope(scope);
                        visit(statement);
                    } finally {
                        scope = scope.getParent();
                    }
                }
            }
        } catch (RuntimeException r) {
            throw new RuntimeException(r);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        try {
            if (ast.getStatements().isEmpty()) {
                throw new RuntimeException("Error: Missing statement");
            }
            
            visit(ast.getValue());
            requireAssignable(Environment.Type.INTEGER_ITERABLE, ast.getValue().getType());

            ast.getStatements().forEach(elem -> {
                try {
                    scope = new Scope(scope);
                    scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);
                } finally {
                    scope = scope.getParent();
                }
            });
        } catch (RuntimeException r) {
            throw new RuntimeException(r);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        try {
            visit(ast.getCondition());
            requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        } catch (RuntimeException r) {
            throw new RuntimeException(r);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        try {
            visit(ast.getValue());

            Environment.Variable ret = scope.lookupVariable("returnType");
            requireAssignable(ret.getType(), ast.getValue().getType());
        } catch (RuntimeException r) {
            throw new RuntimeException(r);
        }


        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        try {
            Object literal = ast.getLiteral();

            switch (literal.getClass().getSimpleName()) {
                case "String":
                    ast.setType(Environment.Type.STRING);
                    break;
                case "Character":
                    ast.setType(Environment.Type.CHARACTER);
                    break;
                case "Nil":
                    ast.setType(Environment.Type.NIL);
                    break;
                case "Boolean":
                    ast.setType(Environment.Type.BOOLEAN);
                    break;
                case "BigInteger":
                    BigInteger temp = (BigInteger) literal;
                    if (temp.intValueExact() > Integer.MAX_VALUE || temp.intValueExact() < Integer.MIN_VALUE) {
                        throw new RuntimeException("Error: Integer range");
                    }
                    ast.setType(Environment.Type.INTEGER);
                    break;
                case "BigDecimal":
                    BigDecimal tempDecimal = (BigDecimal) literal;
                    if (tempDecimal.doubleValue() > Double.MAX_VALUE || tempDecimal.doubleValue() < Double.MIN_VALUE) {
                        throw new RuntimeException("Error: Decimal range");
                    }
                    ast.setType(Environment.Type.DECIMAL);
                    break;
                default:
                    throw new RuntimeException("Error: Bad Type");
            }
        } catch (RuntimeException r) {
            throw new RuntimeException(r);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        try {
            visit(ast.getExpression());
            if (ast.getExpression().getClass() != Ast.Expr.Binary.class) {
                throw new RuntimeException("Error: Bad Type");
            }
        } catch (RuntimeException r) {
            throw new RuntimeException(r);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        try {
            String op = ast.getOperator();
            visit(ast.getLeft());
            visit(ast.getRight());

            switch (op) {
                case "AND":
                case "OR":
                    requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
                    requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());
                    ast.setType(Environment.Type.BOOLEAN);
                    break;
                case "<":
                case "<=":
                case ">":
                case ">=":
                case "==":
                case "!=":
                    requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                    requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                    ast.setType(Environment.Type.BOOLEAN);
                    break;
                case "+":
                    if (ast.getLeft().getType() == Environment.Type.STRING || ast.getRight().getType() == Environment.Type.STRING) {
                        ast.setType(Environment.Type.STRING);
                    } else if ((ast.getLeft().getType() == Environment.Type.INTEGER || ast.getLeft().getType() == Environment.Type.DECIMAL)
                            && ast.getLeft().getType() == ast.getRight().getType()) {
                        ast.setType(ast.getLeft().getType());
                    } else {
                        throw new RuntimeException("Error: Wrong Type +");
                    }
                    break;
                case "-":
                case "*":
                case "/":
                    if ((ast.getLeft().getType() == Environment.Type.INTEGER || ast.getLeft().getType() == Environment.Type.DECIMAL)
                            && ast.getLeft().getType() == ast.getRight().getType()) {
                        ast.setType(ast.getLeft().getType());
                    } else {
                        throw new RuntimeException("Error: Wrong Type *-/");
                    }
                    break;
                default:
                    throw new RuntimeException("Error: Wrong Type Binary");
            }
        } catch (RuntimeException r) {
            throw new RuntimeException(r);
        }


        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        try {
            if (ast.getReceiver().isPresent()) {
                Ast.Expr.Access temp = Ast.Expr.Access.class.cast(ast.getReceiver().get());
                temp.setVariable(scope.lookupVariable(temp.getName()));
                scope = scope.lookupVariable(temp.getName()).getType().getScope();
                ast.setVariable(scope.lookupVariable(ast.getName()));
            }
            else {
                ast.setVariable(scope.lookupVariable(ast.getName()));
            }
        } catch (RuntimeException r) {
            throw new RuntimeException(r);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        throw new UnsupportedOperationException();  // TODO
    }

}
