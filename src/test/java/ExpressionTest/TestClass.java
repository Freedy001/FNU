package ExpressionTest;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Freedy
 * @date 2021/12/16 23:07
 */
@Data
public class TestClass {
    public static String NAME="LIST";
    public static int AGE=200;
    public static TestClass2 testClass2=new TestClass2();

    public static boolean IF=false;

    private int age=100;
    private String name="bzd0";
    private boolean _if=true;
    public TestClass2 t2=new TestClass2();
    public Map<String, String> map=new HashMap<>();
    public List<Object> strList=new ArrayList<>();
    {
        strList.add("cp");
        strList.add(null);
        strList.add(List.of("gaga"));
    }
    public List<Object> list=List.of("haha","pin",
            strList
            ,"out","kidding");

    public static Boolean test(Boolean flag){
        return flag;
    }


    public static Object prsf(){
        return "haha";
    }

    public Object haha(TestClass testClass){
        return testClass.toString();
    }

    public Object haha(int a,TestClass testClass){
        return testClass.toString();
    }

    public Object haha(TestClass testClass,double b){
        return testClass.toString();
    }

    public Object haha(Long a,TestClass testClass){
        return testClass.toString();
    }
}
