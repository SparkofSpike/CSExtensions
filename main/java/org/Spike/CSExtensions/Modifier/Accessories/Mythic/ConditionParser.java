package org.Spike.CSExtensions.Modifier.Accessories.Mythic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConditionParser {
    private final List<Token> tokens;
    private int current;

    public ConditionParser(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            this.tokens = new ArrayList<>();
            this.tokens.add(new Token(Token.Type.EOF));
        } else {
            ConditionLexer lexer = new ConditionLexer(condition);
            this.tokens = lexer.tokenize();
        }
        this.current = 0;
    }

    public boolean evaluate(Set<String> weaponTags) {

        reset();


        if (tokens.size() == 1 && tokens.get(0).getType() == Token.Type.EOF) {
            return true;
        }
        return parseExpression(weaponTags);
    }

    public Set<String> getAllTags() {
        java.util.HashSet<String> tags = new java.util.HashSet<>();
        for (Token token : tokens) {
            if (token.getType() == Token.Type.TAG) {
                tags.add(token.getValue());
            }
        }
        return tags;
    }

    private boolean parseExpression(Set<String> weaponTags) {
        return parseOrExpression(weaponTags);
    }

    private boolean parseOrExpression(Set<String> weaponTags) {
        boolean result = parseAndExpression(weaponTags);

        while (match(Token.Type.OR)) {
            boolean right = parseAndExpression(weaponTags);
            result = result || right;
        }

        return result;
    }

    private boolean parseAndExpression(Set<String> weaponTags) {
        boolean result = parseNotExpression(weaponTags);

        while (match(Token.Type.AND)) {
            boolean right = parseNotExpression(weaponTags);
            result = result && right;
        }

        return result;
    }

    private boolean parseNotExpression(Set<String> weaponTags) {
        if (match(Token.Type.NOT)) {
            return !parseNotExpression(weaponTags);
        }
        return parsePrimary(weaponTags);
    }

    private boolean parsePrimary(Set<String> weaponTags) {
        if (match(Token.Type.LPAREN)) {
            boolean result = parseExpression(weaponTags);
            consume(Token.Type.RPAREN, "Expect ')' after expression.");
            return result;
        }

        if (match(Token.Type.NOT)) {

            if (match(Token.Type.LPAREN)) {
                boolean result = parseExpression(weaponTags);
                consume(Token.Type.RPAREN, "Expect ')' after expression.");
                return !result;
            }

            Token token = consume(Token.Type.TAG, "Expect tag after '!'.");
            return !evaluateTag(token.getValue(), weaponTags);
        }

        if (check(Token.Type.TAG)) {
            Token token = advance();
            return evaluateTag(token.getValue(), weaponTags);
        } else {


            System.err.println("[ConditionParser] 语法错误: 期望标签，但得到 " + peek().getType());

            advance();
            return false;
        }
    }

    private boolean evaluateTag(String tag, Set<String> weaponTags) {
        if ("null".equalsIgnoreCase(tag)) {
            return weaponTags == null || weaponTags.isEmpty();
        }
        if ("all".equalsIgnoreCase(tag)) {
            return true;
        }

        if (weaponTags == null) return false;

        for (String weaponTag : weaponTags) {
            if (tag.equalsIgnoreCase(weaponTag)) {
                return true;
            }
        }
        return false;
    }

    private boolean match(Token.Type type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private Token consume(Token.Type type, String message) {
        if (check(type)) return advance();
        throw new RuntimeException(message);
    }

    private boolean check(Token.Type type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().getType() == Token.Type.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
    public void reset() {
        this.current = 0;
    }

}