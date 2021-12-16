import com.freedy.ServerStarter;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {

    private ServerStarter serverStarter=new ServerStarter();
    private List<Object> serverStarters=new ArrayList<>();
    @SneakyThrows
    public static void main(String[] args) {
        Field field = Test.class.getDeclaredField("serverStarters");
        System.out.println(Arrays.toString(field.getType().getInterfaces()));
        Type type = field.getGenericType();
        System.out.println(type.getTypeName());
        if (type instanceof Class<?> c){
            System.out.println(Arrays.toString(c.getInterfaces()));
        }
        if (type instanceof ParameterizedType parameterizedType){
            System.out.println(parameterizedType.getActualTypeArguments()[0]);
            System.out.println(parameterizedType.getRawType());
        }
    }
}