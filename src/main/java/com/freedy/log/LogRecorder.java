package com.freedy.log;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author Freedy
 * @date 2021/12/1 22:53
 */
public class LogRecorder extends OutputStream {
    private final int SIZE = 10 * 1024 * 1024;
    private final PrintStream systemPrinter;
    private final byte[] buffer = new byte[SIZE];
    private int rIndex, wIndex = 0;


    public LogRecorder(PrintStream systemPrinter) {
        this.systemPrinter = systemPrinter;
    }


    @Override
    public void write(int b) {
        write(new byte[]{(byte) b});
    }


    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        systemPrinter.write(b, off, len);

        //只取b后BUFFER_SIZE长度的数组
        if (len > SIZE) {
            off += len - SIZE;
            int remain = SIZE - wIndex;
            System.arraycopy(b, off, buffer, wIndex, remain);
            System.arraycopy(b, off + remain, buffer, 0, SIZE - remain);
            if (rIndex < wIndex) {
                rIndex = wIndex;
            }
            return;
        }
        int exceed = wIndex + len - SIZE;
        if (exceed > 0) {
            int remain = SIZE - wIndex;
            System.arraycopy(b, off, buffer, wIndex, remain);
            System.arraycopy(b, off + remain, buffer, 0, exceed);
            wIndex = exceed;
            if (rIndex < wIndex) {
                rIndex = wIndex;
            }
            return;
        }

        System.arraycopy(b, off, buffer, wIndex, len);
        wIndex += len;
    }

    public String getLog() {
        if (rIndex == wIndex) return null;
        String log = rIndex < wIndex ? new String(buffer, rIndex, wIndex - rIndex) : new String(buffer, rIndex, SIZE - rIndex) + new String(buffer, 0, wIndex);
        rIndex = wIndex;
        return log;
    }

    public String getLog(int expectLength) {
        String log;
        if (rIndex < wIndex) {
            int min = Math.min(expectLength, wIndex - rIndex);
            log = new String(buffer, rIndex, min);
            rIndex += min;
        } else {
            int remain = SIZE - rIndex;
            if (expectLength < remain) {
                log = new String(buffer, rIndex, expectLength);
                rIndex += expectLength;
            } else {
                String s = new String(buffer, rIndex, remain);
                int min = Math.min(wIndex, expectLength - remain);
                log = s + new String(buffer, 0, min);
                rIndex += min;
            }
        }
        return log;
    }

}
