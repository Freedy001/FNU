import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.StringJoiner;

public class Main {


    public static void main(String[] args) throws IOException {
        InetSocketAddress addr = new InetSocketAddress("172.27.215.84", 0);
        InetSocketAddress maskAddr = new InetSocketAddress("255.255.240.0", 0);
        byte[] ip = addr.getAddress().getAddress();
        byte[] mask = maskAddr.getAddress().getAddress();
        StringJoiner joiner = new StringJoiner(".");
        for (int i = 0; i < 4; i++) {
            int res = Byte.toUnsignedInt(ip[i]) & Byte.toUnsignedInt(mask[i]);
            joiner.add((i == 3 ? res + 1 : res) + "");
        }
        System.out.println(joiner);
    }


}
