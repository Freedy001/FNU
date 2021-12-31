package com.freedy.tinyFramework.Expression.stander;

import com.freedy.tinyFramework.Expression.token.DirectAccessToken;
import com.freedy.tinyFramework.Expression.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Freedy
 * @date 2021/12/30 14:33
 */
public class StanderTokenBlockSorter implements Function<List<List<Token>>,List<List<Token>>> {


    @Override
    public List<List<Token>> apply(List<List<Token>> lists) {
        List<List<Token>> funcList = new ArrayList<>();
        List<List<Token>> normList = new ArrayList<>();
        for (List<Token> stream : lists) {
            if (stream.size() == 1 && stream.get(0) instanceof DirectAccessToken dir && dir != null && "func".equals(dir.getMethodName())) {
                funcList.add(stream);
            } else {
                normList.add(stream);
            }
        }
        for (int i = funcList.size()-1; i >=0; i--) {
            lists.add(0,funcList.get(i));
        }
        funcList.addAll(normList);
        return funcList;
    }
}
