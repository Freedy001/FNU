package com.freedy.tinyFramework.Expression;

import com.freedy.tinyFramework.Expression.token.*;
import com.freedy.tinyFramework.exception.ExpressionSyntaxException;
import com.freedy.tinyFramework.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Freedy
 * @date 2021/12/14 15:28
 */
@Slf4j
public class Tokenizer {

    //                                                  T  (java.lang.Math)  .   pow      (  3 ,    2 )
    private static final Pattern staticPattern = Pattern.compile("(.*?T) *?\\((.*?)\\) *(\\?)? *(\\.)? *?(.*)");
    //                                                        #  test    .   ?      val
    private static final Pattern referencePattern = Pattern.compile("(.*?#) *?(.*?) *?\\. *?(.*)");
    //      static                                                    [1,2,3,4] [1]
    private static final Pattern collectionPattern = Pattern.compile("^(?!\\{.*?}) *?\\[(.*?)](?: *?\\[(.*)])?");
    //      static                                             {a:b,c:d} [2]
    private static final Pattern mapPattern = Pattern.compile("(\\{.*?})(?: *?\\[(.*)])?");

    private static final Pattern strPattern = Pattern.compile("^'(.*?)'$");

    private static final Pattern numericPattern = Pattern.compile("\\d+|\\d+[lL]|\\d+?\\.\\d+");

    private static final Pattern boolPattern = Pattern.compile("true|false");

    private static final Pattern methodPattern = Pattern.compile("(.*?)\\((.*?)\\)");
    //      static                                         T  (java.lang.Math) . ?  pow      (  3 ,    2 )
    private static final Pattern expressionBracket = Pattern.compile(".*?T *?\\($|.*?\\..*?\\w+ *?\\($");
    //      static                                       以[a-zA-Z0-9_]开头
    private static final Pattern varPattern = Pattern.compile("^[a-zA-Z_]\\w*");

    private static final Pattern defPattern = Pattern.compile("^def +?(.*)");
    //[<=>| static !+_*?()]
    private static final Set<Character> operationSet = Set.of('=', '<', '>', '|', '&', '!', '+', '-', '*', '/', '(', ')', '?');
    private static final Set<Character> bracket = Set.of('(', ')');


    public static List<TokenStream> getTokenStreamList(String crossLineEl) {
        return Arrays.stream(crossLineEl.replaceAll("\r\n|\n", " ")
                .split(";")).map(Tokenizer::getTokenStream).collect(Collectors.toCollection(LinkedList::new));
    }

    public static TokenStream getTokenStream(String expression) {
        TokenStream tokenStream = new TokenStream(expression);

        char[] chars = expression.toCharArray();
        final int length = chars.length;

        int lastOps = 0;
        boolean expressionBracket = false;

        boolean insideSubBracket = false;
        int subBracketCounter = 0;
        int totalBracket = getSubElBracketCount(expression);

        for (int i = 0; i < length; i++) {
            char inspectChar = chars[i];

            if (inspectChar == '`') {
                subBracketCounter++;
                insideSubBracket = subBracketCounter != totalBracket;
            }


            if (inspectChar == ' ' || !operationSet.contains(inspectChar) || insideSubBracket) {
                continue;
            }

            String token = expression.substring(lastOps, i).trim();

            checkTernary:
            if (inspectChar == '?') {
                int index = nextNonempty(chars, i);
                if (index == -1) break checkTernary;
                if (chars[index] == '.') break checkTernary;
                if (chars[index] == '?') break checkTernary;

                //尝试构建三元token
                int[] ternaryIndex = {i};
                TernaryToken ternaryToken = buildTernary(expression, ternaryIndex);
                if (ternaryToken == null) continue;

                i = ternaryIndex[0] - 1;
                lastOps = ternaryIndex[0];

                //构建前置token
                buildToken(tokenStream, token);
                //构建前置操作token
                tokenStream.addToken(new OpsToken("?"));
                tokenStream.addToken(ternaryToken);
                continue;
            }

            if (bracket.contains(inspectChar)) {
                //构建token
                if (inspectChar == '(') {
                    if (Tokenizer.expressionBracket.matcher(expression.substring(lastOps, i + 1).trim()).matches()) {
                        expressionBracket = true;
                        continue;
                    }
                    if (StringUtils.hasText(token)) {
                        ExpressionSyntaxException.thr(expression, token + "@(");
                    }
                    tokenStream.addBracket(true);
                }
                if (inspectChar == ')') {
                    if (expressionBracket) {
                        expressionBracket = false;
                        continue;
                    }
                    buildToken(tokenStream, token);
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
            buildToken(tokenStream, token);

            OpsToken opsToken = new OpsToken(inspectChar + "");
            tokenStream.addToken(opsToken);
            lastOps = i + 1;

        }

        //build last
        String token = expression.substring(lastOps, length).trim();
        buildToken(tokenStream, token);


        return tokenStream;
    }

    //a==b? b==c?1:b==c?1:2 : b==c?1:2
    private static TernaryToken buildTernary(String expression, int[] i) {
        int nestCount = 0;
        int leftBracket = 0;
        int divide = -1;
        int end = -1;
        char[] chars = expression.toCharArray();
        for (int index = i[0] + 1; index < chars.length; index++) {
            char c = chars[index];
            if (c == '?') {
                nestCount++;
                continue;
            }
            if (c == ':') {
                if (nestCount != 0) {
                    nestCount--;
                    divide = -2;
                } else {
                    divide = index;
                }
            }
            if (divide < 0) continue;
            if (c == '(') {
                leftBracket++;
                continue;
            }
            if (c == ')') {
                leftBracket--;
            }
            if (leftBracket < 0) {
                end = index;
                break;
            }
        }
        if (end == -1) {
            end = expression.length();
        }
        if (divide == -2) {
            ExpressionSyntaxException.thrWithMsg("illegal ternary expression", expression, "?$" + expression.substring(i[0] + 1));
        }
        if (divide == -1) {
            //没有检测到三元表达式
            return null;
        }
        TernaryToken token = new TernaryToken(expression.substring(i[0] + 1, end));
        try {
            token.setTrueTokenStream(getTokenStream(expression.substring(i[0] + 1, divide)));
            token.setFalseTokenStream(getTokenStream(expression.substring(divide + 1, end)));
        } catch (ExpressionSyntaxException e) {
            new ExpressionSyntaxException(expression)
                    .buildErrorStr(e.getSyntaxErrStr().toArray(new String[0]))
                    .buildToken(e.getLayer().toArray(new Token[0]))
                    .buildMsg("sub expression err")
                    .buildCause(e)
                    .buildConsoleErrorMsg()
                    .thr();
        }
        i[0] = end;
        return token;
    }


    private static void buildToken(TokenStream tokenStream, String token) {
        if (StringUtils.isEmpty(token)) return;
        String expression = tokenStream.getExpression();
        //构建collection Token
        Matcher matcher = collectionPattern.matcher(token);
        if (matcher.find()) {
            CollectionToken collectionToken = new CollectionToken(token);
            String group = matcher.group(1);
            for (String ele : group.split(",")) {
                String newEle = ele.trim();
                if (StringUtils.hasText(newEle)) {
                    if (newEle.startsWith("`") && newEle.endsWith("`")) {
                        newEle = newEle.substring(1, newEle.length() - 1);
                    }
                    try {
                        TokenStream stream = getTokenStream(newEle);
                        collectionToken.addTokenStream(stream);
                    } catch (ExpressionSyntaxException e) {
                        new ExpressionSyntaxException(expression)
                                .buildMsg("sub expression error!")
                                .buildErrorStr(e.getSyntaxErrStr().toArray(new String[0]))
                                .buildCause(e)
                                .buildConsoleErrorMsg()
                                .thr();
                    }
                }
            }

            String ops = matcher.group(2);
            if (StringUtils.hasText(ops)) {
                if (ops.startsWith("`") && ops.endsWith("`")) {
                    ops = ops.substring(1, ops.length() - 1);
                }
                collectionToken.setRelevantOps(getTokenStream(ops));
            }

            tokenStream.addToken(collectionToken);
            return;
        }
        //构建map Token
        matcher = mapPattern.matcher(token);
        if (matcher.find()) {
            MapToken mapToken = new MapToken(token);
            mapToken.setMapStr(matcher.group(1));
            mapToken.setRelevantOpsName(matcher.group(2));
            tokenStream.addToken(mapToken);
            return;
        }
        //构建static Token
        matcher = staticPattern.matcher(token);
        if (matcher.find()) {
            String staticFlag = matcher.group(1).trim();
            if (!staticFlag.equals("T")) {
                ExpressionSyntaxException.thrWithMsg("illegal T() flag", expression, staticFlag.substring(0, staticFlag.length() - 1) + "$T(" + matcher.group(2).trim());
            }
            StaticToken staticToken = new StaticToken(token);
            staticToken.setReference(matcher.group(2).trim());
            staticToken.setCheckMode(StringUtils.hasText(matcher.group(3)));
            //检测是否有点分字符
            if (StringUtils.hasText(matcher.group(4))) {
                //构建执行链
                buildExecuteChain(expression, staticToken, matcher.group(5).trim());
            } else {
                ExpressionSyntaxException.thrWithMsg("static call must specify method or property", expression, staticToken.getReference() + "@)");
            }
            tokenStream.addToken(staticToken);
            return;
        }
        //构建reference Token
        matcher = referencePattern.matcher(token);
        if (matcher.find()) {
            String referenceName = matcher.group(1).trim();
            if (!referenceName.equals("#")) {
                ExpressionSyntaxException.thrWithMsg("illegal # reference", expression, referenceName.substring(0, referenceName.length() - 1) + "$#" + matcher.group(2).trim());
            }
            ReferenceToken referenceToken = new ReferenceToken(token);
            String reference = matcher.group(2).trim();
            boolean checkMode = reference.endsWith("?");
            referenceToken.setReference(checkMode ? reference.substring(0, reference.length() - 1).trim() : reference);
            referenceToken.setCheckMode(checkMode);
            //构建执行链
            buildExecuteChain(expression, referenceToken, matcher.group(3).trim());
            tokenStream.addToken(referenceToken);
            return;
        } else {
            if (token.startsWith("#")) {
                String referenceName = token.substring(1);
                boolean checkMode = referenceName.endsWith("?");
                if (checkMode) {
                    referenceName = referenceName.substring(0, referenceName.length() - 1);
                }
                if (!varPattern.matcher(referenceName).matches()) {
                    ExpressionSyntaxException.thrWithMsg("illegal prop name", expression, "#@" + referenceName);
                }
                ReferenceToken referenceToken = new ReferenceToken(token);
                referenceToken.setReference(referenceName);
                referenceToken.setCheckMode(checkMode);
                tokenStream.addToken(referenceToken);
                return;
            }
        }
        matcher = defPattern.matcher(token);
        if (matcher.find()) {
            ObjectToken objectToken = new ObjectToken(token);
            String varName = matcher.group(1);
            if (!varPattern.matcher(varName).matches()) {
                ExpressionSyntaxException.thrWithMsg("illegal var name", "def@" + varName);
            }
            objectToken.setVariableName(varName);
            tokenStream.addToken(objectToken);
            return;
        }
        //构建 numeric Token
        matcher = numericPattern.matcher(token);
        if (matcher.matches()) {
            BasicVarToken numeric = new BasicVarToken("numeric", token);
            tokenStream.addToken(numeric);
            return;
        }
        //构建string Token
        matcher = strPattern.matcher(token);
        if (matcher.find()) {
            BasicVarToken numeric = new BasicVarToken("str", matcher.group(1));
            tokenStream.addToken(numeric);
            return;
        }
        //构建boolean Token
        matcher = boolPattern.matcher(token);
        if (matcher.matches()) {
            BasicVarToken numeric = new BasicVarToken("bool", token);
            tokenStream.addToken(numeric);
            return;
        }
        //构建变量 Token
        matcher = varPattern.matcher(token);
        if (matcher.matches()) {
            tokenStream.addToken(new NormalVarToken(token));
            return;
        }
        //点分token
        if (token.startsWith(".") && tokenStream.getLastToken().isValue(")")) {
            String suffix = token.substring(1);
            DotSplitToken dotToken = new DotSplitToken(suffix);
            buildExecuteChain(expression, dotToken, suffix);
            tokenStream.addToken(new OpsToken("."));
            tokenStream.addToken(dotToken);
            return;
        }
        //非法token
        ExpressionSyntaxException.thrWithMsg("unrecognized token!", expression, token);
    }

    private static void buildExecuteChain(String expression, ClassToken token, String suffixStr) {
        String[] step = suffixStr.split("\\.");
        for (int i = 0; i < step.length; i++) {
            String propOrMethodName = step[i];
            boolean checkMode = propOrMethodName.trim().endsWith("?");
            if (checkMode) {
                propOrMethodName = propOrMethodName.substring(0, propOrMethodName.length() - 1).trim();
            }
            Matcher matcher = methodPattern.matcher(propOrMethodName);
            if (matcher.find()) {
                String methodName = matcher.group(1).trim();
                if (!varPattern.matcher(methodName).matches()) {
                    StringJoiner joiner = new StringJoiner(".");
                    for (int j = 0; j < i; j++) {
                        joiner.add(step[j]);
                    }
                    ExpressionSyntaxException.thrWithMsg("illegal method name", expression, StringUtils.hasText(joiner.toString()) ? (joiner + ".@" + methodName) : methodName);
                }
                String group = matcher.group(2);
                if (StringUtils.hasText(group)){
                 //todo ` `
                }
                token.addMethod(checkMode, methodName, StringUtils.hasText(group) ? group.split(",") : new String[0]);
            } else {
                if (!varPattern.matcher(propOrMethodName).matches()) {
                    StringJoiner joiner = new StringJoiner(".");
                    for (int j = 0; j < i; j++) {
                        joiner.add(step[j]);
                    }
                    ExpressionSyntaxException.thrWithMsg("illegal prop name", expression, joiner + ".@" + propOrMethodName);
                }
                token.addProperties(checkMode, propOrMethodName);
            }
        }

    }

    private static int getSubElBracketCount(String expression) {
        int count = 0;
        for (char c : expression.toCharArray()) {
            if (c == '`') count++;
        }
        if (count == 1) {
            ExpressionSyntaxException.thr(expression, "`");
        }
        return count;
    }

    private static int nextNonempty(char[] charArray, int cursor) {
        cursor++;
        boolean hasFind = false;
        for (int i = cursor; i < charArray.length; i++) {
            if (charArray[i] == ' ') {
                cursor++;
            } else {
                hasFind = true;
                break;
            }
        }
        return hasFind ? cursor : -1;
    }

}
