package com.freedy.tinyFramework.Expression;

import com.freedy.tinyFramework.Expression.token.Token;
import com.freedy.tinyFramework.exception.IllegalArgumentException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.*;

/**
 * @author Freedy
 * @date 2021/12/14 19:46
 */
public class TokenStream {
    private final List<Token> tokenStream = new ArrayList<>();
    private final Set<String> doubleOps = Set.of("||", "&&", "!=", "==", ">=", "<=", "++", "--", "+=", "-=");
    private final Stack<Integer> bracketStack = new Stack<>();
    @Getter
    private final Queue<Block> priorityBlock = new PriorityQueue<>(Comparator.comparingInt(o -> (o.endIndex - o.startIndex)));

    @Data
    @AllArgsConstructor
    static class Block {
        //(startIndex,endIndex] 左开又闭
        int startIndex;
        int endIndex;
    }

    /**
     * 合并双元操作
     */
    public boolean mergeOps(char currentOps) {
        int size = tokenStream.size();
        if (size == 0) return false;
        Token token = tokenStream.get(size - 1);
        if (token.isType("operation")) {
            String nOps = token.getValue() + currentOps;
            if (doubleOps.contains(nOps)) {
                token.setValue(nOps);
                return true;
            } else {
                throw new IllegalArgumentException("illegal ops ? at the tokenStream index: ?", nOps, size - 1);
            }
        }
        return false;
    }


    public void addBracket(boolean isLeft) {
        if (isLeft) {
            bracketStack.push(tokenStream.size() - 1);
        } else {
            Integer startIndex = bracketStack.pop();
            priorityBlock.add(new Block(startIndex, tokenStream.size() - 1));
        }
    }

    public void addToken(Token token) {
        tokenStream.add(token);
    }

    public List<Token> getInner(){
        return tokenStream;
    }

}
