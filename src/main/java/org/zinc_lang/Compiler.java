package org.zinc_lang;

import com.github.zeroeighteightzero.lwlp.*;

import java.util.ArrayList;
import java.util.List;

public class Compiler {

    private static boolean tokenEquals(Token token, String... values) {
        if (token == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.equals(token.value)) {
                return true;
            }
        }
        return false;
    }

    static BlockScope block(org.zinc_lang.Parser token) {
        if (!token.is("LBRACE")) {
            // TODO error
            return null;
        }
        token.advance();
        ArrayList<StatementNode> body = new ArrayList<>();
        while (!token.is("RBRACE")) { // FIXME Create API for `expect` to throw error when reaching EOF
            StatementNode s = statement(token);
            if (s == null) {
                break;
            }
            body.add(s);
        }
        token.advance();
        BlockScope b = new BlockScope();
        b.nodes = body;
        return b;
    }

    static FuncNode func(org.zinc_lang.Parser token, FuncNode.Visibility visibility) {
        try (Parser.Section s = token.debug("function")) {
            FuncNode f = new FuncNode();
            f.argTypes = new ArrayList<>();
            f.argNames = new ArrayList<>();
            f.args = 0;
            f.visibility = visibility;
            Token name = token.advance().cur();
            if (token.is("IDENTIFIER") && token.advance().is("LPAREN")) {
                f.name = name;
                token.advance();
                boolean close = false;
                while (token.is("IDENTIFIER", "PRIMITIVE")) {
                    f.argTypes.add(type(token));
                    if (!token.is("IDENTIFIER")) {
                        // TODO error
                        return null;
                    }
                    f.argNames.add(token.cur());
                    token.advance();
                    if (!token.is("COMMA") && !token.is("RPAREN")) {
                        // TODO error
                        return null;
                    }
                    f.args++;
                    if (token.is("RPAREN")) {
                        token.advance();
                        close = true;
                        break;
                    }
                    token.advance();
                }
                if (!close && token.is("RPAREN")) {
                    token.advance();
                } else if (!close) {
                    // TODO error
                    return null;
                }
                if (token.is("SEMICOLON")) {
                    token.advance();
                    return f;
                }
                if (!token.is("LBRACE")) {
                    if (token.is("IDENTIFIER", "PRIMITIVE")) {
                        f.retType = type(token);
                        if (token.is("SEMICOLON")) {
                            token.advance();
                            return f;
                        } else if (!token.is("LBRACE")) {
                            // TODO error
                            return null;
                        }
                    } else {
                        // TODO error
                        return null;
                    }
                }
                f.body = block(token);
                return f;

            }
        }
        // TODO error
        return null;
    }

    static IfStatement ifs(org.zinc_lang.Parser token) {
        token.advance();
        IfStatement i = new IfStatement();
        if (token.is("LPAREN")) {
            token.advance();
            i.condition = expr(token);
            if (token.is("RPAREN")) {
                token.advance();
            }
            i.ifBody = block(token);
            return i;

        }
        // TODO error
        return null;
    }

    static ForStatement fors(org.zinc_lang.Parser token) {
        try (org.zinc_lang.Parser.Section s = token.debug("for")) {

            token.advance();
            ForStatement f = new ForStatement();
            if (token.is("LPAREN")) {
                token.advance();
                f.init = declare(token);
                if (f.init == null) {
                    f.init = expr(token);
                }
                if (!token.is("SEMICOLON")) {
                    // TODO error
                    return null;
                }
                f.condition = expr(token.advance());
                if (!token.is("SEMICOLON")) {
                    // TODO error
                    return null;
                }
                f.update = expr(token.advance());
/*
            if (f.update == null) {
                f.update = declare(token);
            }
*/
                if (token.is("RPAREN")) {
                    token.advance();
                } else {
                    // TODO error
                    return null;
                }
                f.body = block(token);
                return f;
            }
        }
        // TODO error
        return null;
    }

    static WhileStatement whiles(org.zinc_lang.Parser token) {
        WhileStatement w = new WhileStatement();
        if (token.is("LPAREN")) {
            token.advance();
            w.condition = expr(token);
            if (token.is("RPAREN")) {
                token.advance();
            }
            w.body = block(token);
            return w;

        }
        // TODO error
        return null;
    }

    public static DeclareNode declare(org.zinc_lang.Parser token) {
        token.snap();
        try (org.zinc_lang.Parser.Section s = token.debug("declare")) {
            TypeVal assignType = type(token);
            Token id = token.cur();
            if (token.is("IDENTIFIER") && token.advance().is("EQUALS")) {
                DeclareNode decl = new DeclareNode();
                decl.type = assignType;
                decl.identifier = id;
                token.advance();
                decl.value = expr(token);
                return decl;
            }
        }
        token.shot();
        return null;
    }

    static FactorNode factor(org.zinc_lang.Parser token) {

        try (org.zinc_lang.Parser.Section s = token.debug("factor")) {

            FactorNode f = new FactorNode();
            if (token.is("NULL", "IDENTIFIER", "STRING", "INTEGER", "DECIMAL", "BOOLEAN")) {
                f.literal = token.cur();
                token.advance();
                if (token.is("OPERATOR") && tokenEquals(token.cur(), "++", "--")) {
                    try (org.zinc_lang.Parser.Section ss = token.debug("unary")) {
                        UnaryExpr u = new UnaryExpr();
                        u.factor = f;
                        u.operator = token.cur().value;
                        u.postfix = true;
                        f = new FactorNode();
                        f.unary = u;
                        f.factorType = FactorNode.FactorType.UNARY;
                        return f;
                    }
                }
                f.factorType = FactorNode.FactorType.LITERAL;
                return f;
            } else if (token.is("LPAREN")) {
                if (!token.advance().is("IDENTIFIER", "PRIMITIVE")) {
                    // TODO error
                    return null;
                }
                TypeVal dest = type(token);
                if (!token.is("RPAREN")) {
                    // TODO error
                    return null;
                }
                FactorNode fac = factor(token.advance());
                CastNode c = new CastNode();
                c.destType = dest;
                c.val = fac;
                f.cast = c;
                f.factorType = FactorNode.FactorType.CAST;
                return f;
            } else if (token.is("OPERATOR")) {
                try (org.zinc_lang.Parser.Section ss = token.debug("unary")) {
                    Token op = token.cur();
                    if (!tokenEquals(op, "-", "+", "&", "*", "!", "~", "--", "++")) {
                        // TODO error
                        return null;
                    }
                    FactorNode fac = factor(token.advance());
                    if (fac != null) {
                        UnaryExpr u = new UnaryExpr();
                        u.factor = fac;
                        u.operator = op.value;
                        u.postfix = false;
                        f.unary = u;
                        f.factorType = FactorNode.FactorType.UNARY;
                        return f;
                    }
                }
            } else if (token.is("LPAREN")) {
                ExprNode e = expr(token.advance());
                if (e != null) {
                    f.expr = e;
                    f.factorType = FactorNode.FactorType.EXPR;
                    token.advance();
                    return f;
                }
            }

            // TODO error
            return null;
        }
    }

    static BinaryTerm term(org.zinc_lang.Parser token) {

        try (org.zinc_lang.Parser.Section s = token.debug("term")) {

            BinaryTerm t = new BinaryTerm();
            FactorNode left = factor(token);
            t.left = left;
            if (left != null) {
                while (token.is("OPERATOR")) {
                    Token op = token.cur();
                    if (tokenEquals(op, "*", "/", "&&")) {
                        FactorNode right = factor(token.advance());
                        if (right == null) {
                            // TODO error
                            return null;
                        }
                        t.right = right;
                        t.op = op.value;
                        BinaryTerm leftTerm = t;
                        t = new BinaryTerm();
                        t.left = leftTerm;
                    } else {
                        break;
                    }
                }
                return t;
            }


            // TODO error
            return null;
        }
    }

    static boolean compMode;

    static ExprNode expr(org.zinc_lang.Parser token) {

        try (org.zinc_lang.Parser.Section s = token.debug("expr")) {

            { // Assign
                token.snap();
                Token id = token.cur();
                if (token.is("IDENTIFIER") && token.advance().is("EQUALS")) {
                    AssignExpr assign = new AssignExpr();
                    assign.identifier = id;
                    assign.value = expr(token.advance());
                    return assign;
                }
                token.shot();
            }

            { // Function Call
                token.snap();
                CallNode c = new CallNode();
                c.name = token.cur();
                c.args = new ArrayList<>();
                if (token.is("IDENTIFIER") && token.advance().is("LPAREN")) {
                    ExprNode e = expr(token.advance());
                    while (e != null) {
                        c.args.add(e);
                        e = null;
                        if (token.is("COMMA")) {
                            e = expr(token.advance());
                        }
                    }
                    if (!token.is("RPAREN")) {
                        // TODO error
                        return null;
                    }
                    token.advance();
                    return c;
                }
                token.shot();
            }

            if (!compMode) { // Comparison // FIXME Super hacky
                token.snap();
                compMode = true;
                ExprNode left = expr(token);
                compMode = false;
                if (left != null && token.is("COMP")) {
                    CompareExpr c = new CompareExpr();
                    c.left = left;
                    c.op = token.cur().value;
                    ExprNode right = expr(token.advance());
                    if (right != null) {
                        c.right = right;
                        return c;
                    } else {
                        // TODO error
                        return null;
                    }
                }
                token.shot();
            }

            { // Binary
                token.snap();
                BinaryExpr e = new BinaryExpr();
                BinaryTerm left = term(token);
                e.left = left;
                if (left != null) {
                    while (token.is("OPERATOR")) {
                        Token op = token.cur();
                        if (tokenEquals(op, "+", "-", "&", "|", "||")) {
                            BinaryTerm right = term(token.advance());
                            if (right == null) {
                                // TODO error
                                return null;
                            }
                            e.right = right;
                            e.op = op.value;
                            //token.advance();
                            BinaryExpr leftExpr = e;
                            e = new BinaryExpr();
                            e.left = leftExpr;
                        }
                    }

                    //token.advance();
                    return e;
                }
                token.shot();
            }

            // TODO error
            return null;
        }

    }

    static TypeVal type(org.zinc_lang.Parser token) {
        if (!token.is("IDENTIFIER", "PRIMITIVE")) {
            // TODO error
            return null;
        }
        TypeVal t = new TypeVal();
        t.builtin = token.is("PRIMITIVE");
        t.type = token.value;
        token.advance();
        while (token.is("OPERATOR") && token.equals("*")) {
            t.pointerLevel++;
            token.advance();
        }
        if (token.is("OPERATOR") && !token.equals("*")) {
            // TODO error
            return null;
        }
        /*ArrayList<Integer> dim = new ArrayList<>(); TODO Arrays
        while (token.is("LBRACKET")) {
            token.advance();
            if (!token.is("RBRACKET", "INTEGER")) {
                // TODO error     array initialization must be constant.
                return null;
            }
            if (token.is("INTEGER")) {
                dim.add(Integer.parseInt(token.value));
                t.dimensions++;
                token.advance();
            }
            if (!token.is("RBRACKET")) {
                // TODO error
                return null;
            }
            token.advance();
        }
        t.arraySizes = new int[t.dimensions];
        for (int i = 0; i < dim.size(); i++) {
            t.arraySizes[i] = dim.get(i);
        }*/
        return t;
    }

    static StatementNode statement(org.zinc_lang.Parser token) {
        if (token.is("SEMICOLON")) {
            token.advance();
            return new StatementNode();
        }
        if (token.is("KEYWORD")) {
            if (token.equals("import")) {
                token.snap();
                if (token.is("IDENTIFIER")) {
                    ImportNode im = new ImportNode();
                    while (token.is("IDENTIFIER")) {
                        im.path += token.value;
                        token.advance();
                        if (!token.is("DOT") || !token.is("SEMICOLON")) {
                            // TODO error
                            return null;
                        }
                        if (token.is("DOT")) {
                            im.path += ".";
                            token.advance();
                        } else {
                            token.advance();
                            break;
                        }
                    }
                    return im;
                }
                token.shot();
            } else if (token.equals("func")) {
                token.snap();
                FuncNode f = func(token, FuncNode.Visibility.PACKAGE);
                if (f != null) return f;
                token.shot();
            } else if (token.equals("private")) {
                token.snap();
                if (token.advance().equals("func")) {
                    FuncNode f = func(token, FuncNode.Visibility.PRIVATE);
                    if (f != null) return f;
                }
                token.shot();
            } else if (token.equals("public")) {
                token.snap();
                if (token.advance().equals("func")) {
                    FuncNode f = func(token, FuncNode.Visibility.PUBLIC);
                    if (f != null) return f;
                }
                token.shot();
            } else if (token.equals("if")) {
                token.snap();
                IfStatement i = ifs(token);
                if (i != null) return i;
                token.shot();
            } else if (token.equals("while")) {
                token.snap();
                WhileStatement w = whiles(token);
                if (w != null) return w;
                token.shot();
            } else if (token.equals("for")) {
                token.snap();
                ForStatement f = fors(token);
                if (f != null) return f;
                token.shot();
            } else if (token.equals("return")) {
                token.snap();
                token.advance();
                if (token.is("SEMICOLON")) {
                    token.advance();
                    return new ReturnNode();
                } else {
                    ExprNode e = expr(token);
                    if (e != null) {
                        ReturnNode r = new ReturnNode();
                        r.value = e;
                        if (!token.is("SEMICOLON")) {
                            // TODO error
                            return null;
                        }
                        token.advance();
                        return r;
                    }
                }
                token.shot();
            } else if (token.equals("break")) {
                token.snap();
                if (!token.advance().is("SEMICOLON")) {
                    // TODO error;
                    return null;
                }
                token.advance();
                return new BreakNode();
            } else if (token.equals("defer")) {
                token.snap();
                token.advance();
                StatementNode s = statement(token);
                if (s != null) {
                    DeferNode d = new DeferNode();
                    d.body = s;
                    if (!token.is("SEMICOLON")) {
                        // TODO error
                        return null;
                    }
                    token.advance();
                    return d;
                }
                token.shot();
            } else if (token.equals("continue")) {
                token.snap();
                if (!token.advance().is("SEMICOLON")) {
                    // TODO error;
                    return null;
                }
                token.advance();
                return new ContinueNode();
            }
        }
        if (token.is("LBRACE")) {
            return block(token);
        }
        if (token.is("IDENTIFIER", "PRIMITIVE")) {
            DeclareNode d = declare(token);
            if (d != null) {
                return d;
            }
        }
        ExprNode e = expr(token);
        if (e != null) {
            if (!token.is("SEMICOLON")) {
                // TODO error
                return null;
            }
            token.advance();
            return e;
        }
        // TODO error
        return null;
    }

    private static final Lexer lexer = new Lexer(new TokenPattern[]{
            new TokenPattern("\\b(if|else|while|for|func|public|private|return|defer|continue|break|struct|class|import)\\b", "KEYWORD"),
            new TokenPattern("\\:", "IN"),
            new TokenPattern("\\b(bool|u8|u16|u32|u64|i8|i16|i32|i64|f32|f64|int|float|void)\\b", "PRIMITIVE"),
            new TokenPattern("(\\>|\\<|(\\=\\=)|(\\!\\=)|(\\>\\=)|(\\<\\=))", "COMP"),
            new TokenPattern("\\\"(\\\\\\\"|[^\\\"])*\\\"", "STRING"),
            new TokenPattern("null", "NULL"),
            new TokenPattern("true|false", "BOOLEAN"),
            new TokenPattern("[a-zA-Z_][a-zA-Z0-9_]*", "IDENTIFIER"),
            new TokenPattern("[+-]?[0-9]+i?", "INTEGER"),
            new TokenPattern("[+-]?(([0-9]+\\.?)|([0-9]*\\.[0-9]+))[fd]?", "DECIMAL"),
            new TokenPattern("((\\+\\+)|(\\-\\-)|\\+|\\-|\\*|\\/|(\\&\\&)|(\\|\\|)|\\&|\\||\\!|\\~)", "OPERATOR"),
            new TokenPattern("\\=", "EQUALS"),
            new TokenPattern("\\.", "DOT"),
            new TokenPattern("\\,", "COMMA"),
            new TokenPattern("\\;", "SEMICOLON"),
            new TokenPattern("\\(", "LPAREN"),
            new TokenPattern("\\)", "RPAREN"),
            new TokenPattern("\\[", "LBRACKET"),
            new TokenPattern("\\]", "RBRACKET"),
            new TokenPattern("\\{", "LBRACE"),
            new TokenPattern("\\}", "RBRACE"),
            new TokenPattern("\\<", "LCHEV"),
            new TokenPattern("\\>", "RCHEV"),
    });

    public static ArrayList<StatementNode> buildAST(String source) {
        source = Preprocessor.process(source);

        List<Token> toks = lexer.lex(source);

        int idx = 0;

        org.zinc_lang.Parser parser = new Parser(idx, toks);
        parser.source = source;

        ArrayList<StatementNode> program = new ArrayList<>();

        Token lastToken = null;
        int repeat = 0;
        while (!parser.end()) {
            if (lastToken == parser.cur()) {
                repeat++;
                if (repeat > 4) {
                    throw new RuntimeException("Failed to compile. (LOOP)");
                }
            } else {
                repeat = 0;
            }
            StatementNode s = statement(parser);
            program.add(s);
            lastToken = parser.cur();
        }

        return program;
    }

}
