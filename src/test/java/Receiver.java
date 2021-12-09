import com.freedy.tinyFramework.utils.ReflectionUtils;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * @author Freedy
 * @date 2021/11/27 11:15
 */
public class Receiver {


    int[] a;

    public static void main(String[] args) throws Exception {
        Class<Integer> integerClass = int.class;

        Object newArray = Array.newInstance(integerClass, 3);

        Array.set(newArray,0, ReflectionUtils.convertType("523", integerClass));
        Array.set(newArray,1, ReflectionUtils.convertType("12", integerClass));
        Array.set(newArray,2, ReflectionUtils.convertType("43", integerClass));

        Receiver scratch = new Receiver();
        Receiver.class.getDeclaredField("a").set(scratch,newArray);
        System.out.println(Arrays.toString(scratch.a));
    }

}
