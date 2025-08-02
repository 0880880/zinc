package org.zinc_lang;

public class DeferNode extends StatementNode {

     public StatementNode body;

    @Override
    public String toString() {
        return String.format("defer %s;", body);
    }
}
