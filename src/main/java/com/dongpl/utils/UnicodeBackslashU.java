package com.dongpl.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnicodeBackslashU {

    private static final String singlePattern = "[0-9|a-f|A-F]";

    private static final String pattern = singlePattern + singlePattern + singlePattern +singlePattern;

    private static String ustartToCn(final String str) {
        StringBuilder sb = new StringBuilder().append("0x").append(str.substring(2, 6));
        Integer codeInteger = Integer.decode(sb.toString());
        int code = codeInteger.intValue();
        char c = (char) code;
        return String.valueOf(c);
    }

    private static boolean isStartWithUnicode(final String str) {
        if (null == str || str.length() == 0) {
            return false;
        }
        if (!str.startsWith("\\u")) {
            return false;
        }
        if (str.length() < 6) {
            return false;
        }
        String content = str.substring(2,6);
        return Pattern.matches(pattern,content);
    }

    public static String unicodeToCn(final String str) {
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        for(int i = 0; i <length;) {
            String tmpStr = str.substring(i);
            if (isStartWithUnicode(tmpStr)) {
                sb.append(ustartToCn(tmpStr));
                i += 6;
            } else {
                sb.append(str.substring(i,i+1));
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * 将中文和部分符号转成unicode编码
     */
    public static String cnToUnicode(final String str) {
        String regex = "[\u4e00-\u9fa5\\pP&&[^#:./=-]]"; // 除之外的标点符号进行转义
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result,String.format("\\\\u%04x",(int) matcher.group().charAt(0)));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
