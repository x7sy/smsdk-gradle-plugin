package com.smwl.smsdk;

/**
 * Created on 2021/5/13.
 *
 * @author Linzugeng
 */
public class Log {
    public static void i(String msg) {
        System.out.println("[SMSDK_INFO]" + msg);
    }

    public static void i(String tag, String msg) {
        System.out.println("[SMSDK_INFO][" + tag + "]" + msg);
    }

    public static void e(String msg) {
        System.err.println("[SMSDK_ERROR]" + msg);
    }

    public static void e(String tag, String msg) {
        System.err.println("[SMSDK_ERROR][" + tag + "]" + msg);
    }
}
