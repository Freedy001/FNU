import lombok.SneakyThrows;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.util.Map;


public class Test {

    private Map<Integer, Integer> map;

    @SneakyThrows
    public static void main(String[] args) throws SecurityException, NoSuchFieldException {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(com.freedy.manage.Test .class);
        enhancer.setCallback((MethodInterceptor) (obj, method, args1, proxy) -> {
            System.out.println("before method run...");
            Object result = proxy.invokeSuper(obj, args1);
            System.out.println("after method run...");
            return result;
        });
        Object sample =  enhancer.create();
        System.out.println(sample);
    }

}