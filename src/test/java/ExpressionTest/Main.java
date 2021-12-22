package ExpressionTest;


import com.freedy.tinyFramework.Expression.Expression;
import com.freedy.tinyFramework.Expression.ExpressionPasser;
import com.freedy.tinyFramework.Expression.StanderEvaluationContext;
import com.freedy.tinyFramework.utils.PlaceholderParser;
import lombok.SneakyThrows;

import java.util.Scanner;
import java.util.StringJoiner;
import java.util.function.Supplier;

/**
 * @author Freedy
 * @date 2021/12/16 23:07
 */
public class Main {

    public static ExpressionPasser passer = new ExpressionPasser();
    public static StanderEvaluationContext context = new StanderEvaluationContext(new TestClass());
    private static TestClass test = new TestClass();
    private static TestClass2 class2 = new TestClass2();
    private static TestClass3 class3 = new TestClass3();

    static {
        context.setVariable("test", test);
        context.setVariable("class2", class2);
        context.setVariable("class3", class3);
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
    @SneakyThrows
    public static void main(String[] args) throws InterruptedException {
//        evaluate("T(ExpressionTest.TestClass).testClass2.getName()");
//        evaluate("T(ExpressionTest.TestClass).NAME='haha'",()->TestClass.NAME);
//        evaluate("T(ExpressionTest.TestClass).AGE++",()->TestClass.AGE);
//        evaluate("T(ExpressionTest.TestClass).AGE--",()->TestClass.AGE);
//        evaluate("++T(ExpressionTest.TestClass).AGE",()->TestClass.AGE);
//        evaluate("--T(ExpressionTest.TestClass).AGE",()->TestClass.AGE);
//
//        evaluate("!T(ExpressionTest.TestClass).IF",()->TestClass.IF);
//        evaluate("T(ExpressionTest.TestClass).prsf()='gaga'", TestClass::prsf);
//
//        evaluate("#test.age+=100",()->test.getAge());
//        evaluate("++#test.age",()->test.getAge());
//        evaluate("#test.age/=1000",()->test.getAge());
//        evaluate("--#test.age",()->test.getAge());
//        //100+102
//        evaluate("#test.age++ + ++#test.age", () -> test.getAge());
//        //102+102
//        evaluate("#test.age++ + --#test.age", () -> test.getAge());
//        //102+102
//        evaluate("#test.age-- + ++#test.age", () -> test.getAge());
//        //102+100
//        evaluate("#test.age-- + --#test.age", () -> test.getAge());
//
//        evaluate("#test.age++ - ++#test.age", () -> test.getAge());
//        evaluate("#test.age++ - --#test.age", () -> test.getAge());
//        evaluate("#test.age-- - ++#test.age", () -> test.getAge());
//        evaluate("#test.age-- - --#test.age", () -> test.getAge());
//        evaluate("#test.name='nihao'",()->test.getName());

//        evaluate("#test._if=(!T(ExpressionTest.TestClass).IF||#test._if&&false)", () -> test.is_if());
//        evaluate("#test?.abxc");
//        evaluate("#test?.age");
//        evaluate("#test?.name.replace('bzd','haha')");
//        evaluate("#tes?._if");
//        evaluate("#tes?.haha()");
//        evaluate("def haha='nihao'",()->test.getAge());
//        evaluate("#haha",()->test.getAge());
        evaluate("""
                def map={'haha':'gege'};
                for i in 10:({
                    #map.put({#i+'haha'},'haha');
                });
                for entry in #map.entrySet():({
                    T(java.lang.System).out.println(#entry);
                });
                """);
        System.out.println();

//        evaluate("""
//                def abc=asdasdasd;
//
//                abc==sfd;
//                if a==23:
//
//                for i in range:
//                abc={}
//                ;
//
//                """);

//        scan();

        //try {
        //                                return token.calculateResult(ANY_TYPE);
        //                            } catch (Exception e) {
        //                                throw new EvaluateException("calculate ? result failed",e).errToken(token);
        //                            }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @SneakyThrows
    public static void scan() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("EL-commander-line@FNU # ");
            String line = scanner.nextLine();
            evaluate(line);
            System.out.println("\n\n");
        }
    }

    @SafeVarargs
    public static void evaluate(String expression, Supplier<Object>... suppliers) throws InterruptedException {
        try {
            StringJoiner joiner = new StringJoiner(",", "(", ")");
            for (Supplier<Object> supplier : suppliers) {
                joiner.add(String.valueOf(supplier.get()));
            }
            System.out.println(new PlaceholderParser("?", "======>relevant expression>>>>>>>>" + expression + "======>relevantStr>>>>>>>>>>>>>>>>>" + joiner + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>").configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_CYAN));
            Expression ex = passer.parseExpression(expression);
            System.out.println(new PlaceholderParser("?*", ex.getValue(context)).configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_YELLOW));
            joiner = new StringJoiner(",", "(", ")");
            for (Supplier<Object> supplier : suppliers) {
                joiner.add(String.valueOf(supplier.get()));
            }
            System.out.println(new PlaceholderParser("?", "======>relevantStr>>>>>>>>>>>>>>>>>" + joiner + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n\n").configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_CYAN));
        } catch (Exception e) {
            e.printStackTrace();
            Thread.sleep(1000);
        }
    }
}
