package com.dongpl.utils;

import java.io.UnsupportedEncodingException;
import java.util.Random;

public class RandomChinese {
    /**
     * 获取汉字(繁体字)
     */
    public static String getRandomChinese(int count) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            char c = (char) (random.nextInt(40869 - 19968 + 1) + 19968);
            sb.append(c);
        }
        return sb.toString();
    }

    public static String getSingleChinese(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            String chinese = getSingleChinese();
            sb.append(chinese);
        }
        return sb.toString();
    }

    public static String getSingleChinese() {
        Random random = new Random();
        int highPos, lowPos;
        highPos = (176 + Math.abs(random.nextInt(39)));
        lowPos = (161 + Math.abs(random.nextInt(93)));
        byte[] bArr = new byte[2];
        bArr[0] = (new Integer(highPos)).byteValue();
        bArr[1] = (new Integer(lowPos)).byteValue();
        String chinese = "";
        try {
            chinese = new String(bArr, "GBK");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chinese;
    }

}
