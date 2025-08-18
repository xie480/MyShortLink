package org.yilena.myShortLink.project.utils;

public class Base62Encoder {
    private static final char[] BASE62_CHARS = 
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    
    /**
     * 将长整型数值编码为指定长度的Base62字符串
     */
    public static String encode(long value, int length) {
        char[] buffer = new char[length];
        for (int i = length - 1; i >= 0; i--) {
            buffer[i] = BASE62_CHARS[(int)(value % 62)];
            value /= 62;
        }
        return new String(buffer);
    }
}