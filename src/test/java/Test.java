import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

/**
 * @author Freedy
 * @date 2021/11/27 11:10
 */
public class Test {
    public static void main(String[] args) throws IOException {
        if (args.length!=1||args[0]==null| Objects.equals(args[0], "")) {
            System.out.println("err");
            return;
        }

        ServerSocket socket = new ServerSocket(9090);

        Socket transfer = socket.accept();
        OutputStream outputStream = transfer.getOutputStream();

        FileInputStream inputStream = new FileInputStream(args[0]);

        inputStream.transferTo(outputStream);


        inputStream.close();
        outputStream.close();
        socket.close();
    }
}
