package com.freedy.tinyFramework.Expression;

import com.alibaba.fastjson.JSON;
import com.freedy.tinyFramework.Expression.token.*;
import com.freedy.tinyFramework.exception.ExpressionSyntaxException;
import com.freedy.tinyFramework.exception.IllegalArgumentException;
import com.freedy.tinyFramework.utils.StringUtils;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/14 15:28
 */
public class Tokenizer {

    //                                                  T  (java.lang.Math) . ?  pow      (  3 ,    2 )
    private final Pattern staticPattern = Pattern.compile("(.*?T) *?\\((.*?)\\) *?\\. *(\\??) *?(.*)");
    //                                                        #  test    .   ?      val
    private final Pattern referencePattern = Pattern.compile("(.*?#) *?(.*?) *?\\. *(\\??) *?(.*)");
    //                                                         [1,2,3,4] [1]
    private final Pattern collectionPattern = Pattern.compile("^(?!\\{.*?}) *?\\[(.*?)](?: *?\\[(.*)])?");
    //                                                  {a:b,c:d} [2]
    private final Pattern mapPattern = Pattern.compile("(\\{.*?})(?: *?\\[(.*)])?");

    private final Pattern strPattern = Pattern.compile("^'(.*?)'$");

    private final Pattern numeric = Pattern.compile("\\d+|\\d+[lL]");

    private final Pattern methodPattern = Pattern.compile("(.*?)\\((.*?)\\)");
    //                                              T  (java.lang.Math) . ?  pow      (  3 ,    2 )
    private final Pattern expressionBracket = Pattern.compile(".*?T *?\\($|.*?\\..*?\\w+ *?\\($");

    private final Pattern startWithSymbol = Pattern.compile("^[^a-zA-Z_].*");
    //                                            [a-zA-Z0-9_]
    private final Pattern varPattern = Pattern.compile("\\w+");

    //[<=>|&!+_*?()]
    private final Set<Character> operationSet = Set.of('=', '<', '>', '|', '&', '!', '+', '-', '*', '/', '(', ')');
    private final Set<Character> bracket = Set.of('(', ')');


    public static void main(String[] args) {
        TokenStream stream = new Tokenizer().getTokenStream(
                "#test.enabled=(T(com.freedy.Context).INTRANET_CHANNEL_RETRY_TIMES==#localProp.enabled)>{1,2,3,4,5}[0]");
        for (Token token : stream.getInfixExpression()) {
            System.out.println(JSON.toJSONString(token));
        }
        stream.calculateSuffix();
        System.out.println("==================================");
        for (Token token : stream.calculateSuffix()) {
            System.out.println(JSON.toJSONString(token));
        }
    }

    /*
      true
      false
      #test.srt++*2 =='abc'
      #test.val>2+3+2*5-T(java.util.Math).pow(3,2)
      #test.enabled=(T(com.freedy.Context).INTRANET_CHANNEL_RETRY_TIMES==#localProp.enabled)>{1,2,3,4,5}[0]
      T(com.freedy.Context).test()
      #localProp.enabled
      #localProp.init()
      #localProp?.enabled
      #localProp?.init()
      [[1],[22,3,4],[3],[4],[5]][1]=[1]
      {'k1':'123','k2':'321'}
      =
      ==
      <
      >
      ||
      &&
    */
    public TokenStream getTokenStream(String expression) {
        expression = expression.replaceAll("\r\n|\n", " ");
        TokenStream tokenStream = new TokenStream(expression);

        char[] chars = expression.toCharArray();
        final int length = chars.length;

        int lastOps = 0;
        boolean expressionBracket = false;
        for (int i = 0; i < length; i++) {
            char inspectChar = chars[i];

            if (inspectChar == ' ' || !operationSet.contains(inspectChar)) {
                continue;
            }


            String token = expression.substring(lastOps, i).trim();
            if (bracket.contains(inspectChar)) {
                //构建token
                if (inspectChar == '(') {
                    if (this.expressionBracket.matcher(expression.substring(lastOps, i + 1).trim()).matches()) {
                        expressionBracket = true;
                        continue;
                    }
                    if (StringUtils.hasText(token)){
                        ExpressionSyntaxException.thr(expression,token);
                    }
                    tokenStream.addBracket(true);
                }
                if (inspectChar == ')') {
                    if (expressionBracket) {
                        expressionBracket = false;
                        continue;
                    }
                    if (buildToken(tokenStream, token)) {
                        //构建失败
                        ExpressionSyntaxException.thr(expression,token);
                    }
                    tokenStream.addBracket(false);
                }
                lastOps = i + 1;
                continue;
            }

            //如果是双操作符，合并双操作符
            if (token.length() == 0 && tokenStream.mergeOps(inspectChar)) {
                lastOps = i + 1;
                continue;
            }


            //构建token
            if (buildToken(tokenStream, token)) {
                //构建失败
                ExpressionSyntaxException.thr(expression,token);
            }

            OpsToken opsToken = new OpsToken(inspectChar + "");
            tokenStream.addToken(opsToken);
            lastOps = i + 1;

        }
        //build last
        String token = expression.substring(lastOps, length).trim();
        if (buildToken(tokenStream, token)) {
            //构建失败
            ExpressionSyntaxException.thr(expression,token);
        }


        return tokenStream;
    }

    /**
     * @return 是否构建失败
     */
    private boolean buildToken(TokenStream tokenStream, String token) {
        if (StringUtils.isEmpty(token)) return false;
        Matcher matcher = staticPattern.matcher(token);
        if (matcher.find()) {
            StaticToken staticToken = new StaticToken(token);
            if (!matcher.group(1).trim().equals("T")) {
                throw new IllegalArgumentException("illegal expression ?,please check ? part", token, matcher.group(1));
            }
            staticToken.setOpsClass(matcher.group(2).trim());
            staticToken.setNullCheck(matcher.group(3).equals("?"));
            String propOrMethod = matcher.group(4).trim();
            matcher = methodPattern.matcher(propOrMethod);
            if (matcher.find()) {
                //method
                staticToken.setMethodName(matcher.group(1).trim());
                staticToken.setMethodArgs(matcher.group(2).split(","));
            } else {
                staticToken.setPropertyName(propOrMethod);
            }
            tokenStream.addToken(staticToken);
            return false;
        }
        matcher = referencePattern.matcher(token);
        if (matcher.find()) {
            if (!matcher.group(1).trim().equals("#")) {
                throw new IllegalArgumentException("illegal expression ?,please check ? part", token, matcher.group(1));
            }
            ReferenceToken referenceToken = new ReferenceToken(token);
            referenceToken.setReferenceName(matcher.group(2).trim());
            referenceToken.setNullCheck(matcher.group(3).equals("?"));
            String propOrMethod = matcher.group(4).trim();
            matcher = methodPattern.matcher(propOrMethod);
            if (matcher.find()) {
                //method
                referenceToken.setMethodName(matcher.group(1).trim());
                referenceToken.setMethodArgs(matcher.group(2).split(","));
            } else {
                referenceToken.setPropertyName(propOrMethod);
            }
            tokenStream.addToken(referenceToken);
            return false;
        }
        matcher = collectionPattern.matcher(token);
        if (matcher.find()) {
            CollectionToken collectionToken = new CollectionToken(token);
            collectionToken.setElements(matcher.group(1).split(","));
            collectionToken.setRelevantOpsName(matcher.group(2));
            tokenStream.addToken(collectionToken);
            return false;
        }
        matcher = mapPattern.matcher(token);
        if (matcher.find()) {
            MapToken mapToken = new MapToken(token);
            mapToken.setMapStr(matcher.group(1));
            mapToken.setRelevantOpsName(matcher.group(2));
            tokenStream.addToken(mapToken);
            return false;
        }
        matcher = numeric.matcher(token);
        if (matcher.matches()) {
            BasicVarToken numeric = new BasicVarToken("numeric", token);
            tokenStream.addToken(numeric);
            return false;
        }
        matcher = strPattern.matcher(token);
        if (matcher.find()) {
            BasicVarToken numeric = new BasicVarToken("str", matcher.group(1));
            tokenStream.addToken(numeric);
            return false;
        }
        matcher = startWithSymbol.matcher(token);
        if (matcher.matches()) return true;
        matcher = varPattern.matcher(token);
        if (!matcher.matches()) return true;
        tokenStream.addToken(new NormalVarToken(token));
        return false;
    }

}
