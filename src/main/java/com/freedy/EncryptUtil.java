package com.freedy;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class EncryptUtil {

    // 加密
    public static byte[] Encrypt(byte[] data, String sKey) {
        if (sKey == null) throw new IllegalArgumentException("Key为空null");
        // 判断Key是否为16位
        if (sKey.length() != 16) throw new IllegalArgumentException("Key长度不是16位");
        byte[] raw = sKey.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");//"算法/模式/补码方式"
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            return cipher.doFinal(data);//此处使用BASE64做转码功能，同时能起到2次加密的作用。
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    // 解密
    public static byte[] Decrypt(byte[] data, String sKey) {
        try {
            // 判断Key是否正确
            if (sKey == null) throw new IllegalArgumentException("Key为空null");
            // 判断Key是否为16位
            if (sKey.length() != 16) throw new IllegalArgumentException("Key长度不是16位");
            byte[] raw = sKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String stringToMD5(String plainText) {
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance("md5").digest(
                    plainText.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有这个md5算法！");
        }
        StringBuilder md5code = new StringBuilder(new BigInteger(1, secretBytes).toString(16));
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code.insert(0, "0");
        }
        return md5code.toString();
    }
}