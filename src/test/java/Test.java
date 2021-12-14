import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

    public static void main(String[] args) {
        Matcher matcher = Pattern.compile("\\{.*?,?.?}")
                .matcher("{12:21,32:43}");
        while (matcher.find()) {
            System.out.println("============group-" + matcher.groupCount() + "-start============");
            for (int i = 0; i <= matcher.groupCount(); i++) {
//            System.out.println(Arrays.toString(matcher.group(i).split(",")));
                System.out.println(matcher.group(i));
            }
        }
    }
}