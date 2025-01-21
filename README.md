# Custom Language Compiler

This project is a Java-based compiler that implements a custom programming language. It features a complete pipeline for lexing, parsing, interpreting, and executing source code, providing dynamic typing, object-oriented features, and support for first-class functions.

## Features

### Lexer
The `Lexer` class processes raw input into tokens using pattern matching and error handling for constructs like invalid escapes or unterminated strings. Tokens are represented by the `Token` class, which associates each token with:
- **Type**: The category of the token (e.g., `IDENTIFIER`, `INTEGER`, `STRING`).
- **Literal**: The actual value of the token.
- **Index**: The token's position in the source code.

### Abstract Syntax Tree (AST)
The `Ast` class defines the hierarchical structure of the language, supporting:
- **Fields**: Variables with optional initial values.
- **Methods**: Functions with parameters and statements.
- **Statements and Expressions**: Control flow, assignments, and operations.

### Interpreter
The `Interpreter` class directly evaluates the AST, executing operations and managing control flow. Key features include:
- Support for conditional (`if`), loop (`for`, `while`), and return statements.
- Evaluation of expressions such as arithmetic, logical, and string concatenation.
- Management of runtime scope for variables and functions.

### Scope
The `Scope` class manages nested variable and function definitions, enabling:
- **Variable resolution**: Handles lookup, declaration, and updates.
- **Function resolution**: Supports function declarations and invocations with strict arity checks.

### Environment
The `Environment` class provides runtime object and function management, enabling:
- Dynamic typing for variables.
- Object-oriented features like method calls and fields.
- First-class functions with flexible argument handling.

## Token Types
The language supports the following token types:
- `IDENTIFIER`: Names for variables, methods, or fields.
- `INTEGER`: Whole numbers.
- `DECIMAL`: Numbers with fractional parts.
- `CHARACTER`: Single-character literals.
- `STRING`: String literals.
- `OPERATOR`: Arithmetic, logical, and comparison operators.

## Example Usage

1. **Lexing**: Break down the source code into tokens using the `Lexer` class.
2. **Parsing**: Generate an AST from the tokens.
3. **Interpreting**: Evaluate the AST using the `Interpreter`.

## Installation and Usage
1. Clone the repository:
   ```bash
   git clone https://github.com/your-repo/custom-language-compiler.git
