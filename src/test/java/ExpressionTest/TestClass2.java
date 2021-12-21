package ExpressionTest;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author Freedy
 * @date 2021/12/16 23:13
 */
@Data
public class TestClass2 {
    public String name="TestClass2";
    public int age=0;
    public boolean gender;
    public double weight;
    public List<String> list;
    public Map<String, String> map;
    public TestClass testClass;
    public TestClass3 t3;


}
