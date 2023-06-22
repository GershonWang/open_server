package com.dongpl.utils;

import com.dongpl.entity.FileEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileUtil {

    public static List<File> getFiles(String path) {
        List<File> list = new ArrayList<>();
        File file = new File(path);
        if (!file.exists()) {
            return list;
        }
        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            return list;
        }
        for (File f : files) {
            String name = f.getName();
            if (name.endsWith(".properties") && !name.equals("bootstrap.properties") && !name.equals("application.properties")) {
                list.add(f);
            }
        }
        return list;
    }

    public static List<Map<String, FileEntity>> getFileList(String path) {
        List<Map<String, FileEntity>> list = new ArrayList<>();
        File file = new File(path);
        if (!file.exists()) {
            return list;
        }
        File[] files = file.listFiles();
        if (files == null) {
            return list;
        }
        for (File f : files) {
            String name = f.getName();
            if (name.endsWith(".properties") && name.startsWith("application-") && !name.equals("bootstrap.properties") && !name.equals("application.properties")) {
                Map<String, FileEntity> map = new HashMap<>();
                FileEntity fileEntity = new FileEntity();
                fileEntity.setFileName(name);
                fileEntity.setPath(f.getPath());
                fileEntity.setParentPath(f.getParent());
                String realName = name.split(".properties")[0];
                String serverName = realName.split("-")[1];
                fileEntity.setServerName(serverName);
                fileEntity.setOpen(false);
                String port = PropertiesUtil.getType(f.getPath(), "server.port");
                fileEntity.setPort(port);
                map.put(serverName,fileEntity);
                list.add(map);
            }
        }
        return list;
    }

    public static List<String> getFileNames(String path) {
        File file = new File(path);
        if (!file.exists()){
            return null;
        }
        List<String> fileNames = new ArrayList<>();
        return getFileNames(file,fileNames);
    }

    private static List<String> getFileNames(File file, List<String> fileNames) {
        File[] files = file.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                getFileNames(f,fileNames);
            } else if (f.getName().endsWith(".bat")) {
                fileNames.add(f.getName().split(".bat")[0]);
            }
        }
        return fileNames;
    }

}
