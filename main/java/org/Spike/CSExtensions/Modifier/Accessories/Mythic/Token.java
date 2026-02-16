package org.Spike.CSExtensions.Modifier.Accessories.Mythic;

public class Token {
    public enum Type {
        TAG,
        AND,
        OR,
        NOT,
        LPAREN,
        RPAREN,
        EOF         // 结束
    }

    private final Type type;
    private final String value;

    public Token(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public Token(Type type) {
        this(type, "");
    }

    public Type getType() { return type; }
    public String getValue() { return value; }

    @Override
    public String toString() {
        return type + (value.isEmpty() ? "" : "(" + value + ")");
    }
}