package org.Spike.CSExtensions.Modifier.Accessories.Mythic;

import java.util.ArrayList;
import java.util.List;

public class ConditionLexer {
    private final String input;
    private int position;

    public ConditionLexer(String input) {
        this.input = input != null ? input.trim() : "";
        this.position = 0;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        System.out.println("[ConditionLexer] 输入: '" + input + "' 长度: " + input.length());
        while (position < input.length()) {
            char current = input.charAt(position);
            System.out.println("[ConditionLexer] 位置: " + position + " 字符: '" + current + "'");
            if (Character.isWhitespace(current)) {
                position++;
                continue;
            }

            if (current == '&' && peekNext() == '&') {
                tokens.add(new Token(Token.Type.AND));
                position += 2;
            } else if (current == '|' && peekNext() == '|') {
                tokens.add(new Token(Token.Type.OR));
                position += 2;
            } else if (current == '!') {
                tokens.add(new Token(Token.Type.NOT));
                position++;
            } else if (current == '(') {
                tokens.add(new Token(Token.Type.LPAREN));
                position++;
            } else if (current == ')') {
                tokens.add(new Token(Token.Type.RPAREN));
                position++;
            } else {

                StringBuilder tag = new StringBuilder();
                while (position < input.length()) {
                    char c = input.charAt(position);
                    if (c == '&' || c == '|' || c == '!' || c == '(' || c == ')' || Character.isWhitespace(c)) {
                        break;
                    }
                    tag.append(c);
                    position++;
                }

                if (tag.length() > 0) {
                    tokens.add(new Token(Token.Type.TAG, tag.toString().toLowerCase()));
                } else {

                    position++;
                }
            }
            System.out.println("[ConditionLexer] 当前tokens: " + tokens.size());
        }

        tokens.add(new Token(Token.Type.EOF));
        System.out.println("[ConditionLexer] 最终tokens: " + tokens);
        return tokens;
    }

    private char peekNext() {
        return position + 1 < input.length() ? input.charAt(position + 1) : '\0';
    }
}