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
    private String name;
    private int age=0;
    private boolean gender;
    private double weight;
    private List<String> list;
    private Map<String, String> map;
    private TestClass testClass;



}
