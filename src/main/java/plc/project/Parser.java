package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        try {
            List<Ast.Field> field = new ArrayList<>();
            List<Ast.Method> method = new ArrayList<>();

            while (peek(Token.Type.IDENTIFIER)) {
                if (peek("LET")) {
                    while (peek("LET")) {
                        field.add(parseField());
                    }
                }
                if (peek("DEF")) {
                    while (peek("DEF")) {
                        method.add(parseMethod());
                    }
                }
            }

            return new Ast.Source(field, method);

        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        try {
            match("LET");
            String name = "";
            String typeName = "";

            if (peek(Token.Type.IDENTIFIER)) {
                name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
            } else {
                if (tokens.has(0))
                    throw new ParseException("No identifier: " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                else
                    throw errMsg("No identifier: ");
            }

            if (peek(":")) {
                match(":");
            } else {
                if (tokens.has(0))
                    throw new ParseException("No operator: " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                else
                    throw errMsg("No operator: ");
            }

            if (peek(Token.Type.IDENTIFIER)) {
                typeName = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
            } else {
                if (tokens.has(0))
                    throw new ParseException("No type: " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                else
                    throw errMsg("No type: ");
            }

            if (peek("=")) {
                match("=");
                Ast.Expr value = parseExpression();
                if (peek(";")) {
                    match(";");

                    return new Ast.Field(name, typeName, Optional.of(value));
                } else {
                    if (tokens.has(0))
                        throw new ParseException("No semicolon: " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                    else
                        throw errMsg("No semicolon: ");
                }
            } else {
                if (peek(";")) {
                    match(";");

                    return new Ast.Field(name, typeName, Optional.empty());
                } else {
                    if (tokens.has(0))
                        throw new ParseException("No semicolon: " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                    else
                        throw errMsg("No semicolon: ");
                }
            }
        }
        catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        try {
            List<String> parameters = new ArrayList<>();
            List<String> parametersType = new ArrayList<String>();
            String returnType = "";
            List<Ast.Stmt> statements = new ArrayList<>();
            String name = "";

            match("DEF");

            if (peek(Token.Type.IDENTIFIER)) {
                name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
            }
            else {
                throw errMsg("No IDENTIFIER: ");
            }

            if (peek("(")) {
                match("(");
            }
            else {
                throw errMsg(("No Parenthesis: "));
            }

            while (peek(Token.Type.IDENTIFIER)) {
                parameters.add(tokens.get(0).getLiteral());
                match(Token.Type.IDENTIFIER);

                if (peek(":")) {
                    match(":");
                } else {
                    if (tokens.has(0))
                        throw new ParseException("No operator: " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                    else
                        throw errMsg("No operator: ");
                }

                if (peek(Token.Type.IDENTIFIER)) {
                    parametersType.add(tokens.get(0).getLiteral());
                    match(Token.Type.IDENTIFIER);
                } else {
                    if (tokens.has(0))
                        throw new ParseException("No type: " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                    else
                        throw errMsg("No type: ");
                }

                if (peek(",")) {
                    match(",");
                    if (peek(")"))
                        throw new ParseException("Trailing comma: " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                } else {
                    if (!peek(")")) {
                        if (tokens.has(0))
                            throw new ParseException("No comma: " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                        else
                            throw errMsg("No comma: ");
                    }
                }
            }

            if (peek(")")) {
                match(")");
            }
            else {
                if (tokens.has(0))
                    throw new ParseException("No Parenthesis: " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                else
                    throw errMsg(("No Parenthesis: "));
            }

            if (peek(":")) {
                match(":");

                if (peek(Token.Type.IDENTIFIER)) {
                    returnType = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                } else {
                    if (tokens.has(0))
                        throw new ParseException("No type: " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                    else
                        throw errMsg("No type: ");
                }
            }

            if (peek("DO"))
                match("DO");
            else {
                if (tokens.has(0))
                    throw new ParseException("No DO: " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                else
                    throw errMsg("No DO: ");
            }
            while (!peek("END")) {
                statements.add(parseStatement());
            }

            if (peek("END")) {
                match("END");
                if (returnType.equals("")) {
                    return new Ast.Method(name, parameters, parametersType, Optional.empty(), statements);
                }
                else {
                    return new Ast.Method(name, parameters, parametersType, Optional.of(returnType), statements);
                }
            } else {
                if (tokens.has(0))
                    throw new ParseException("No END: " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                else
                    throw errMsg("No END: ");
            }

        }
        catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        try {
            if (peek("LET")) {
                return parseDeclarationStatement();
            } else if (peek("IF")) {
                return parseIfStatement();
            } else if (peek("FOR")) {
                return parseForStatement();
            } else if (peek("WHILE")) {
                return parseWhileStatement();
            } else if (peek("RETURN")) {
                return parseReturnStatement();
            } else {
                Ast.Expr expr = parseExpression();
                if (match("=")) {
                    Ast.Expr value = parseExpression();
                    if (match(";")) {
                        return new Ast.Stmt.Assignment(expr, value);
                    } else {
                        if (tokens.has(0))
                            throw new ParseException("No semicolon: " +
                                    tokens.get(0).getIndex(), tokens.get(0).getIndex());
                        else
                            throw new ParseException("No semicolon: " + tokens.get(-
                                    1).getIndex(), tokens.get(-1).getIndex());
                    }
                } else {
                    if (match(";")) {
                        return new Ast.Stmt.Expression(expr);
                    } else {
                        if (tokens.has(0))
                            throw new ParseException("No semicolon: " +
                                    tokens.get(0).getIndex(), tokens.get(0).getIndex());
                        else
                            throw new ParseException("No semicolon: " + tokens.get(-
                                    1).getIndex(), tokens.get(-1).getIndex());
                    }
                }
            }
        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        try {
            match("LET");
            String name = "";

            if (peek(Token.Type.IDENTIFIER)) {
                name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
            }

            if (match("=")) {
                Ast.Expr value = parseExpression();
                if (match(";")) {
                    return new Ast.Stmt.Declaration(name, Optional.of(value));
                }
            } else {
                if (match(";")) {
                    return new Ast.Stmt.Declaration(name, Optional.empty());
                }
            }

            throw errMsg("Exception ID ");
        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        try {
            List<Ast.Stmt> thenStmt = new ArrayList<>();
            List<Ast.Stmt> elseStmt= new ArrayList<>();

            match("IF");

            Ast.Expr expr = parseExpression();

            if (match("DO")) {
                while (!peek("ELSE") && !peek("END")) {
                    thenStmt.add(parseStatement());
                }
                if (match("ELSE")) {
                    while (!peek("END"))
                        elseStmt.add(parseStatement());
                }
                if (peek("END")) {
                    match("END");
                    return new Ast.Stmt.If(expr, thenStmt, elseStmt);
                }
            }

            throw errMsg(("No DO: "));

        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        try {
            match("FOR");
            String name = "";

            if (peek(Token.Type.IDENTIFIER)) {
                name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);

                if (peek("IN")) {
                    match("IN");
                }
                else {
                    throw errMsg("No IN ");
                }

                Ast.Expr value = parseExpression();

                if (peek("DO")) {
                    match("DO");
                }
                else {
                    throw errMsg("No DO ");
                }

                List<Ast.Stmt> statements = new ArrayList<>();

                while (!peek("END")) {
                    statements.add(parseStatement());
                }

                if (peek("END")) {
                    match("END");
                    return new Ast.Stmt.For(name, value, statements);
                }
                else {
                    throw errMsg("No END ");
                }
            } else {
                throw errMsg("No IDENTIFIER ");
            }
        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        try {
            match("WHILE");
            List<Ast.Stmt> statements = new ArrayList<>();
            Ast.Expr condition = parseExpression();

            if (peek("DO")) {
                match("DO");
            }
            else {
                throw errMsg("No DO ");
            }

            while (!peek("END")) {
                statements.add(parseStatement());
            }

            if (match("END")) {
                return new Ast.Stmt.While(condition, statements);
            }
            else {
                throw errMsg("Exception ID ");
            }
        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        try {
            match("RETURN");

            Ast.Expr value = parseExpression();

            if (peek(";")) {
                match(";");
                return new Ast.Stmt.Return(value);
            }

            throw errMsg("Exception ID ");
        } catch (ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        Ast.Expr left = parseEqualityExpression();

        while (match("AND") || match("OR")) {
            Token op = tokens.get(-1);

            Ast.Expr right = parseEqualityExpression();

            left = new Ast.Expr.Binary(op.getLiteral(), left, right);
        }

        return left;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        Ast.Expr left = parseAdditiveExpression();

        while (match("<") || match("<=") || match(">") || match(">=")
        || match("==") || match("!=")) {
            Token op = tokens.get(-1);

            Ast.Expr right = parseAdditiveExpression();

            left = new Ast.Expr.Binary(op.getLiteral(), left, right);
        }

        return left;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr left = parseMultiplicativeExpression();

        while (match("+") || match("-")) {
            Token op = tokens.get(-1);

            Ast.Expr right = parseMultiplicativeExpression();

            left = new Ast.Expr.Binary(op.getLiteral(), left, right);
        }

        return left;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr left = parseSecondaryExpression();

        while (match("*") || match("/")) {
            Token op = tokens.get(-1);

            Ast.Expr right = parseSecondaryExpression();

            left = new Ast.Expr.Binary(op.getLiteral(), left, right);
        }

        return left;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        Ast.Expr expr = parsePrimaryExpression();

        while (match(".")) {
            if (!match(Token.Type.IDENTIFIER)) {
                throw new ParseException("No identifier: ", tokens.get(0).getIndex());
            }

            String name = tokens.get(-1).getLiteral();

            if (!match("(")) {
                expr = new Ast.Expr.Access(Optional.of(expr), name);
            } else {
                List<Ast.Expr> args = new ArrayList<>();

                while (!match(")")) {
                    args.add(parseExpression());
                    if (match(",")) {
                        args.add(parseExpression());
                    }
                }

                if (tokens.has(0) && !match(")")) {
                    throw new ParseException("No identifier: " + tokens.get(-1).getIndex(), tokens.get(-1).getIndex());
                }

                expr = new Ast.Expr.Function(Optional.of(expr), name, args);
            }
        }

        return expr;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {
        if (match("NIL")) {
            return new Ast.Expr.Literal(null);
        }
        else if (match("TRUE")) {
            return new Ast.Expr.Literal(true);
        }
        else if (match("FALSE")) {
            return new Ast.Expr.Literal(false);
        }
        else if (match(Token.Type.INTEGER)) {
            return new Ast.Expr.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expr.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.CHARACTER)) {
            String s = noEscape();

            return new Ast.Expr.Literal(s.charAt(1));
        }
        else if (match(Token.Type.STRING)) {
            String s = noEscape();
            s = s.substring(1, s.length() - 1);

            return new Ast.Expr.Literal(s);
        }
        else if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            List<Ast.Expr> args = new ArrayList<>();

            if (match("(")) {
                if (!peek(")")) {
                    args.add(parseExpression());

                    while (match(",")) {
                        args.add(parseExpression());
                    }
                }
                if (match(")")) {
                    return new Ast.Expr.Function(Optional.empty(), name, args);
                }
                else {
                    throw errMsg("No Parenthesis: ");
                }
            }
            else {
                return new Ast.Expr.Access(Optional.empty(), name);
            }
        }
        else if (match("(")) {
            Ast.Expr expr = parseExpression();
            if (!match(")")) {
                throw errMsg("No Parenthesis: ");
            }
            return new Ast.Expr.Group(expr);
        }
        else {
            throw new ParseException("No token: " + tokens.get(-1).getIndex(), tokens.get(-1).getIndex());
        }
    }

    private String noEscape() {
        String s = tokens.get(-1).getLiteral();

        s = s.replace("\\b", "\b");
        s = s.replace("\\n", "\n");
        s = s.replace("\\r", "\r");
        s = s.replace("\\t", "\t");
        s = s.replace("\\'", "'");
        s = s.replace("\\\"", "\"");
        s = s.replace("\\\\", "\\");

        return s;
    }

    private ParseException errMsg(String message) {
        if (tokens.has(0)) {
            return new ParseException(message + tokens.get(0).getIndex(), tokens.get(0).getIndex());
        } else {
            return new ParseException(message + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()),
                    (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }


    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
