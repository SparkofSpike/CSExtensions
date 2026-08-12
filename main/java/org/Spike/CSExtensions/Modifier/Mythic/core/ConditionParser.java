package org.Spike.CSExtensions.Modifier.Mythic.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConditionParser {
    private final String condition;
    private final List<Token> tokens;
    private Set<String> weaponTags;

    public ConditionParser(String condition) {
        this.condition = condition;
        this.tokens = tokenize(condition);
    }

    private enum TokenType {
        TAG, AND, OR, NOT, LPAREN, RPAREN, EOF
    }

    private static class Token {
        TokenType type;
        String value;
        Token(TokenType type, String value) { this.type = type; this.value = value; }
        Token(TokenType type) { this(type, ""); }
    }

    private List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;

        while (pos < input.length()) {
            char c = input.charAt(pos);

            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }

            if (c == '&' && pos + 1 < input.length() && input.charAt(pos + 1) == '&') {
                tokens.add(new Token(TokenType.AND));
                pos += 2;
            } else if (c == '|' && pos + 1 < input.length() && input.charAt(pos + 1) == '|') {
                tokens.add(new Token(TokenType.OR));
                pos += 2;
            } else if (c == '!') {
                tokens.add(new Token(TokenType.NOT));
                pos++;
            } else if (c == '(') {
                tokens.add(new Token(TokenType.LPAREN));
                pos++;
            } else if (c == ')') {
                tokens.add(new Token(TokenType.RPAREN));
                pos++;
            } else {
                StringBuilder tag = new StringBuilder();
                while (pos < input.length()) {
                    char nc = input.charAt(pos);
                    if (nc == '&' || nc == '|' || nc == '!' || nc == '(' || nc == ')' || Character.isWhitespace(nc)) {
                        break;
                    }
                    tag.append(nc);
                    pos++;
                }
                if (tag.length() > 0) {
                    tokens.add(new Token(TokenType.TAG, tag.toString().toLowerCase()));
                }
            }
        }

        tokens.add(new Token(TokenType.EOF));
        return tokens;
    }

    public boolean evaluate(Set<String> weaponTags) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }
        this.weaponTags = weaponTags;
        return parseExpression(0).result;
    }

    private ParseResult parseExpression(int index) {
        return parseOr(index);
    }

    private ParseResult parseOr(int index) {
        ParseResult left = parseAnd(index);
        while (left.index < tokens.size() && tokens.get(left.index).type == TokenType.OR) {
            ParseResult right = parseAnd(left.index + 1);
            left = new ParseResult(left.index = right.index, left.result || right.result);
        }
        return left;
    }

    private ParseResult parseAnd(int index) {
        ParseResult left = parseNot(index);
        while (left.index < tokens.size() && tokens.get(left.index).type == TokenType.AND) {
            ParseResult right = parseNot(left.index + 1);
            left = new ParseResult(left.index = right.index, left.result && right.result);
        }
        return left;
    }

    private ParseResult parseNot(int index) {
        if (tokens.get(index).type == TokenType.NOT) {
            ParseResult inner = parseNot(index + 1);
            return new ParseResult(inner.index, !inner.result);
        }
        return parsePrimary(index);
    }

    private ParseResult parsePrimary(int index) {
        Token token = tokens.get(index);

        if (token.type == TokenType.LPAREN) {
            ParseResult inner = parseExpression(index + 1);
            if (tokens.get(inner.index).type == TokenType.RPAREN) {
                return new ParseResult(inner.index + 1, inner.result);
            }
            return new ParseResult(inner.index, false);
        }

        if (token.type == TokenType.TAG) {
            boolean value = evaluateTag(token.value);
            return new ParseResult(index + 1, value);
        }

        return new ParseResult(index + 1, false);
    }

    private boolean evaluateTag(String tag) {
        if ("null".equals(tag)) {
            return weaponTags == null || weaponTags.isEmpty();
        }
        if ("all".equals(tag)) {
            return true;
        }
        return weaponTags != null && weaponTags.contains(tag);
    }

    private static class ParseResult {
        int index;
        boolean result;
        ParseResult(int index, boolean result) { this.index = index; this.result = result; }
    }

    public Set<String> getAllTags() {
        Set<String> tags = new HashSet<>();
        for (Token token : tokens) {
            if (token.type == TokenType.TAG) {
                tags.add(token.value);
            }
        }
        return tags;
    }
}