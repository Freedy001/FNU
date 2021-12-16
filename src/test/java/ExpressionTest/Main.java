package ExpressionTest;

import com.freedy.Context;
import com.freedy.tinyFramework.Expression.Expression;
import com.freedy.tinyFramework.Expression.ExpressionPasser;
import com.freedy.tinyFramework.Expression.StanderEvaluationContext;

import java.util.StringJoiner;

/**
 * @author Freedy
 * @date 2021/12/16 23:07
 */
public class Main {

    public static ExpressionPasser passer = new ExpressionPasser();
    public static StanderEvaluationContext context = new StanderEvaluationContext(new TestClass());

    static {
        context.setVariable("test", new TestClass());
        context.setVariable("class", new TestClass2());
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
    public static void main(String[] args) {

        evaluate("T(ExpressionTest.TestClass).NAME");
        evaluate("T(ExpressionTest.TestClass).NAME='haha'",TestClass.NAME);
        evaluate("T(ExpressionTest.TestClass).AGE++",TestClass.AGE);
        evaluate("!T(ExpressionTest.TestClass).IF",TestClass.IF);
        evaluate("!T(ExpressionTest.TestClass).prsf()",TestClass.prsf());
        evaluate("#test.age++");
        evaluate("#test.name");


        System.out.println(Context.TEST);
    }

    public static void evaluate(String expression,Object ...relevantStr) {
        Expression ex = passer.parseExpression(expression);
        System.out.println(ex.getValue(context));
        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (Object s : relevantStr) {
            joiner.add(String.valueOf(s));
        }
        System.out.println("======>relevantStr>>>>>>>>>>>>>>>>>"+joiner+">>>>>>>>>>>>>>>>>");
    }
}
