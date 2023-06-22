package com.dongpl.utils;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class PropertiesUtil {

    static Properties properties = new Properties();

    public static String getType(String path,String key) {
        String value = "";
        try {
            BufferedInputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(path)));
            properties.load(in);
            value = properties.getProperty(key);
            in.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return value;
    }
}
