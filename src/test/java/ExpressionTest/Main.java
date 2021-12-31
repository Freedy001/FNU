package ExpressionTest;


import com.freedy.tinyFramework.Expression.Expression;
import com.freedy.tinyFramework.Expression.ExpressionPasser;
import com.freedy.tinyFramework.Expression.StanderEvaluationContext;
import com.freedy.tinyFramework.utils.PlaceholderParser;
import lombok.SneakyThrows;

import java.util.ArrayList;
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
        context.setVariable("scanner", new Scanner(System.in));
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
        long startTime = System.currentTimeMillis();
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
/*        evaluate("""







//                    def a=[1,2,3,4,5,6];
//                    #test[2][3][4][5]
//                    if {1>0} : (print('haha'));
//                    for i in 1:(print(#i));
//                    print(1);
//                    print(true);
//                    print(false);
//                    (#a).size();



//                    def list=new('java.util.ArrayList',2);
//                    func('hello','name','age',@block{
//                        print(#list);
//                        print(#name);
//                        print(#age);
//                        return ({ 'nima' + #name + #age });
//                    });
//                    #list.add('xiao zhao');
//                    #list.add('xiao gege');
//
//                    print(class(#list).getName());

//                    for i in #list.elementData : ({
//                        print({hello('小明','12')});
//                        print(#i);
//                        print('-------------------------------');
//                    });
//                    print(#list.elementData.length);


//                  for i in T(java.lang.Long).MAX_VALUE : ({
//                  })
//                        print('gaga');
//                        return ('nima');
//                        print('gaga');
//                        print('gaga');
//                for i in T(java.lang.Long).MAX_VALUE:({
//                    T(java.lang.System).out.print('输入正则表达式: ');
//                    def pattern=T(java.util.regex.Pattern).compile(#scanner.nextLine());
//                    for j in T(java.lang.Long).MAX_VALUE:({
//                        T(java.lang.System).out.print('输入匹配字符串: ');
//                        def s=#scanner.nextLine();
//                        if {#s.equals('0')}:({
//                            break;
//                        });
//                        def matcher = #pattern.matcher(#s);
//                        if #matcher.find() :({
//                            for groupCount in #matcher.groupCount():({
//                                T(java.lang.System).out.println({'第'+#groupCount+'组匹配:'+#matcher.group(#groupCount)});
//                            });
//                        });
//                    });
//                });
                """);*/
//        Scanner.class.getConstructor().newInstance();
//        System.out.println();

/*        evaluate("""
                def abc=123;
                def bcd=233;
                def arg='test';
                                
                func('test','arg',@block{
                    print('------------test-------------');
                    def abc='inner';
                    print(#abc);
                    print(@abc);
                    print(#bcd);
                    print(@bcd);
                    print(#arg);
                    print(@arg);
                    
                    innerFunc('i am arg');
                });

                                
                test('test args');
                                
                func('innerFunc','arg',@block{
                        print('------------innerFunc-------------');
                        def bcd='innerFunc';
                        print(#abc);
                        print(@abc);
                        print(#bcd);
                        print(@bcd);
                        print(#arg);
                        print(@arg);
                        #abc='!!';
                        @abc='!!';
                });
                                
                print(@abc);
                                

                """);*/

        ArrayList<Integer> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add((int) (Math.random()*1000));
        }

        int size = list.size();
        for (int i = size-1; i >= 1; i--) {
            for (int j = 0; j < i; j++) {
                if (list.get(j)>list.get(j+1)){
                    Integer temp = list.get(j);
                    list.set(j,list.get(j+1));
                    list.set(j+1,temp);
                }
                System.out.println(list);
            }
        }
        System.out.println(System.currentTimeMillis()-startTime);
        startTime=System.currentTimeMillis();
        System.in.read();
        evaluate("""
                def list=[];
                for(i : 1000){
                    def num=T(java.lang.Math).random()*1000;
                    #list.add();
                };
                func('sort','list',@block{
                    def size=#list.size();
                    for(turn @ range(1,#size-1)){
                        for(i : #turn){
                            if(#list[#i]>#list[#i+1]){
                                swap(#list,#i,#i+1);
                            };
                        };
                        print(#list);
                    };
                });

                func('swap','list','a','b',@block{
                    def temp=#list[#a];
                    #list[#a]=#list[#b];
                    #list[#b]=#temp;
                });
                sort(#list);
                """);

        System.out.println("总执行时长:" + (System.currentTimeMillis() - startTime));
        scan();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @SneakyThrows
    public static void scan() {
        Scanner scanner = new Scanner(System.in);
        StringBuilder builder = new StringBuilder();
        boolean blockMode = false;
        while (true) {
            System.out.print(blockMode ? "......................> " : "EL-commander-line@FNU # ");
            String line = scanner.nextLine();
            builder.append(line).append("\n");
            if (line.endsWith("{")) {
                blockMode = true;
            }
            if (line.matches("} *?(?:\\);|\\))$")) {
                blockMode = false;
            }
            if (!blockMode) {
                try {
                    System.out.println(passer.parseExpression(builder.toString()).getValue(context));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("\n");
                builder = new StringBuilder();
            }
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
