package org.zinc_lang;

public class WhileStatement extends StatementNode {

    ExprNode condition;

    public StatementNode body;

    @Override
    public String toString() {
        return String.format("while (%s) %s", condition, body);
    }

}
