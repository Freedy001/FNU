import lombok.SneakyThrows;

import java.util.concurrent.locks.LockSupport;

public class Test {


    @SneakyThrows
    public static void main(String[] args) {
//        Thread thread = new Thread(() -> {
//        });
        long l = System.currentTimeMillis();
        LockSupport.parkNanos(1_000_000_000L);
        System.out.println(System.currentTimeMillis()-l);

//        thread.start();
//        System.in.read();
//        thread.interrupt();
    }


}