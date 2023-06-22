package com.dongpl.controller;

import com.dongpl.entity.FileEntity;
import com.dongpl.utils.FileUtil;
import com.dongpl.utils.UnicodeBackslashU;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import lombok.SneakyThrows;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController implements Initializable {

    @FXML
    public FlowPane pane;
    @FXML
    public MenuBar menuBar;
    @FXML
    public ButtonBar buttonBar;
    @FXML
    public TextArea textArea;
    public Menu fileMenu;
    public Menu confMenu;
    public Menu aboutMenu;

    @SneakyThrows
    @Override
    public void initialize(URL location, ResourceBundle resource) {
        menuBar.prefWidthProperty().bind(pane.widthProperty()); //菜单栏宽度绑定为pane宽度
        buttonBar.prefWidthProperty().bind(pane.widthProperty()); //按钮栏宽度绑定为pane宽度
        textArea.prefWidthProperty().bind(pane.widthProperty()); //文本域宽度绑定为pane宽度
        setButtonBar(buttonBar);
    }

    /**
     * 设置服务按钮列表
     */
    private void setButtonBar(ButtonBar buttonBar) throws IOException {
        ObservableList<Node> buttons = buttonBar.getButtons();
        String property = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + "emss-data";
        List<Map<String, FileEntity>> fileList = FileUtil.getFileList(property);
        for (Map<String,FileEntity> map : fileList) {
            for (String str : map.keySet()) {
                FileEntity entity = map.get(str);
                Button button = new Button(str.toUpperCase());
                button.setStyle("-fx-background-color: green");
                entity.setOpen(false);
                String port = entity.getPort();
                if (port != null && !"".equals(port)) {
                    // 根据端口号查看进程是否存在
                    Process process = Runtime.getRuntime().exec("netstat -ano");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("GBK")));
                    String line;
                    Pattern pattern = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+");
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            String partStr = matcher.group().split(":")[1];
                            if (Objects.equals(partStr,port)) {
                                button.setStyle("-fx-background-color: red;");
                                entity.setOpen(true);
                            }
                        }
                    }
                }
                button.setOnMouseClicked(event -> {
                    String name = event.getButton().name();
                    if ("PRIMARY".equals(name)) {
                        clickLeft(button,entity);
                    } else if ("SECONDARY".equals(name)) {
                        clickRight(entity);
                    }
                });
                buttons.add(button);
            }
        }
    }

    /**
     * 左键点击，启停数据服务
     */
    private void clickLeft(Button button, FileEntity entity) {
        String port = entity.getPort();
        if (port == null || port.equals("")) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setContentText("当前服务端口号为空无法加载，请重新修改配置文件！！！");
            alert.show();
            return;
        }
        long startTime = entity.getStartTime();
        long nowTime = System.currentTimeMillis();
        if (startTime != 0 && nowTime - startTime <= 10000) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setContentText("当前服务正在启动中，请稍后停止！！！");
            alert.show();
            return;
        }
        String parent = entity.getParentPath();
        boolean isOpen = entity.isOpen();
        String fileName = entity.getFileName();
        String[] names = fileName.split(".properties");
        String realName = names[0];
        String serverName = realName.split("-")[1];
        try {
            String systemOS = System.getProperty("os.name");
            System.out.println("当前系统版本：：" + systemOS);
            if (systemOS.toLowerCase().contains("windows")) {
                System.out.println("当前执行windows系统的操作");
                if (isOpen) {
                    // 获取系统进程列表 tasklist
                    Process process = Runtime.getRuntime().exec("netstat -ano");//netstat -ano | findstr 11397
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("GBK")));
                    String line;
                    Pattern pattern = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+");
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            String partStr = matcher.group().split(":")[1];
                            if (Objects.equals(partStr, port)) {
                                String pid = line.split("\\s")[5];
                                if (!"0".equals(pid)) {
                                    //根据进程id杀死进程
                                    Process killProcess = Runtime.getRuntime().exec("taskkill /F /PID " + pid);
                                    killProcess.waitFor();
                                    // runtime正常输出
                                    StringBuilder sb = new StringBuilder();
                                    BufferedReader br = new BufferedReader(new InputStreamReader(killProcess.getInputStream(), Charset.forName("GBK")));
                                    String linee;
                                    while ((linee = br.readLine()) != null) {
                                        textArea.setText(""); //每次打印前清空文本域
                                        sb.append(linee).append("\n");
                                        String body = sb.toString();
                                        String cn = UnicodeBackslashU.unicodeToCn(body);
                                        cn += "\"" + serverName.toUpperCase() + "\"服务停止成功！！！";
                                        textArea.setText(cn);
                                        button.setStyle("-fx-background-color: green;");
                                        entity.setOpen(false);
                                    }
                                    // runtime出现异常
                                    StringBuilder errorSB = new StringBuilder();
                                    BufferedReader errorBR = new BufferedReader(new InputStreamReader(killProcess.getErrorStream(), Charset.forName("GBK")));
                                    String errorLine;
                                    while ((errorLine = errorBR.readLine()) != null) {
                                        textArea.setText(""); //每次打印前清空文本域
                                        errorSB.append(errorLine).append("\n");
                                        String body = errorSB.toString();
                                        String cn = UnicodeBackslashU.unicodeToCn(body);
                                        cn += "\"" + serverName.toUpperCase() + "\"服务停止异常！！！";
                                        textArea.setText(cn);
                                        button.setStyle("-fx-background-color: red;");
                                        entity.setOpen(true);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    String[] cdRoom = new String[] {"cmd","/C","cd /d " + parent + " && java -jar -Dspring.profiles.active=" + serverName + " common-data-2.0.6.jar"};
                    Process process = Runtime.getRuntime().exec(cdRoom);
                    // 获取子进程的输入流和错误流
                    InputStream inputStream = process.getInputStream();
                    InputStream errorStream = process.getErrorStream();
                    // 开启两个线程
                    new Thread(() -> {
                        try {
                            long millis = System.currentTimeMillis();
                            entity.setStartTime(millis);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("GBK")));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                System.out.println(line);
                                // TODO 写入日志文件吧，直接写入界面会因为加载导致异常崩溃
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).start();
                    new Thread(() -> {
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream, Charset.forName("GBK")));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                System.out.println(line);
                                textArea.setText(line);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).start();
                    textArea.setText("\"" + serverName.toUpperCase() + "\"服务启动成功！！！");
                    button.setStyle("-fx-background-color: red;");
                    entity.setOpen(true);
                }
            } else if (systemOS.toLowerCase().contains("linux")){
                System.out.println("当前执行linux系统的操作");
                if (isOpen) {
                    System.out.println("执行关闭服务操作");
                    button.setStyle("-fx-background-color: green;");
                    entity.setOpen(false);
                } else {
                    System.out.println("执行开启服务操作");
//                    String[] cdRoom = new String[]{"/bin/sh","-C","xterm -e cd " + parent};
                    String[] cdRoom = new String[]{"/bin/sh","-C","cd " + parent + " && ls -l"};
                    Process process = Runtime.getRuntime().exec(cdRoom);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("GBK")));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                    button.setStyle("-fx-background-color: red;");
                    entity.setOpen(true);
                }
            } else {
                System.out.println("当前是系统非主流版本");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 右键点击，打开配置文件内容
     */
    private void clickRight(FileEntity entity) {
        try {
            BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(Paths.get(entity.getPath())));
            StringBuilder sb = new StringBuilder();
            int data;
            while ((data = bis.read()) != -1) {
                sb.append((char) data);
            }
            String body = sb.toString();
            String cn = UnicodeBackslashU.unicodeToCn(body);
            textArea.setText(cn);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
