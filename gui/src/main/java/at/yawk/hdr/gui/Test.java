package at.yawk.hdr.gui;

import java.util.Arrays;

/**
 * @author yawkat
 */
public class Test {
    public static void main(String[] args) throws InterruptedException {
        try {
            Arrays.hashCode(new long[Integer.MAX_VALUE - 10]);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        System.out.println(Arrays.toString(args));
    }
}
