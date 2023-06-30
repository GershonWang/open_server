package com.dongpl.utils;

import com.dongpl.entity.FileEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileUtil {

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
            if (name.endsWith(".properties") && !name.equals("bootstrap.properties") && !name.equals("application.properties") && !name.equals("template.properties")) {
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

    public static List<String> getServerPorts(String path) {
        List<String> list = new ArrayList<>();
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
            if (name.endsWith(".properties") && !name.equals("bootstrap.properties") && !name.equals("application.properties") && !name.equals("template.properties")) {
                String port = PropertiesUtil.getType(path + File.separator + name, "server.port");
                list.add(port);
            }
        }
        return list.stream().distinct().collect(Collectors.toList());
    }

    public static List<String> getServerNames(String path) {
        List<String> list = new ArrayList<>();
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
            if (name.endsWith(".properties") && !name.equals("bootstrap.properties") && !name.equals("application.properties") && !name.equals("template.properties")) {
                String applicationName = PropertiesUtil.getType(path + File.separator + name, "spring.application.name");
                String serverName = applicationName.split("-")[1];
                list.add(serverName);
            }
        }
        return list.stream().distinct().collect(Collectors.toList());
    }

    public static void createJar(String path){
        File file = new File(path);
        if (!file.exists()){
            if (file.mkdirs()) {
                String modelPath = path + File.separator + "open_server-1.0-SNAPSHOT.jar";
                String proPath = path + File.separator + "bootstrap.properties";
                try {
                    copyFile("template/open_server-1.0-SNAPSHOT.jar",modelPath);
                    copyFile("template/bootstrap.properties",proPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                AlertUtil.errAlert("当前目录无权限创建目录");
            }
        } else {
            String modelPath = path + File.separator + "open_server-1.0-SNAPSHOT.jar";
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                try {
                    copyFile("template/open_server-1.0-SNAPSHOT.jar",modelPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            String proPath = path + File.separator + "bootstrap.properties";
            File proFile = new File(proPath);
            if (!proFile.exists()) {
                try {
                    copyFile("template/bootstrap.properties",proPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void copyFile(String source,String target) throws IOException {
        InputStream inputStream = FileUtil.class.getClassLoader().getResourceAsStream(source);
        FileOutputStream outputStream = new FileOutputStream(target);
        byte[] buffer = new byte[1024];
        int length;
        if (inputStream != null) {
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer,0,length);
            }
        }
        outputStream.close();
        if (inputStream != null) {
            inputStream.close();
        }
    }

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
