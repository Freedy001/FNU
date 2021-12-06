import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * @author Freedy
 * @date 2021/11/27 11:15
 */
public class Receiver {

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("106.14.177.142", 2323);

        InputStream inputStream = socket.getInputStream();

        FileOutputStream outputStream = new FileOutputStream("C:\\Users\\Freedy\\Desktop\\code\\netUtils\\src\\test\\test.log");

        inputStream.transferTo(outputStream);

        inputStream.close();
        outputStream.close();
        socket.close();
    }

}
