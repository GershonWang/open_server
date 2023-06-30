package com.dongpl.controller;

import com.dongpl.entity.FileEntity;
import com.dongpl.utils.AlertUtil;
import com.dongpl.utils.FileUtil;
import com.dongpl.utils.RandomChinese;
import com.dongpl.utils.UnicodeBackslashU;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController implements Initializable {

    @FXML
    public VBox box;
    @FXML
    public MenuBar menuBar;
    @FXML
    public FlowPane serviceButtonsPane;
    @FXML
    public GridPane textPane;

    public TextArea textArea;

    public Menu changeServiceMenu;
    public Menu deleteServiceMenu;
    public Menu viewServiceLogMenu;
    public static String path = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + "data";

    @Override
    public void initialize(URL location, ResourceBundle resource) {
        menuBar.prefWidthProperty().bind(box.widthProperty()); //菜单栏宽度绑定为box宽度
        serviceButtonsPane.prefWidthProperty().bind(box.widthProperty()); //flowPane宽度绑定为box宽度
        setFlowPane(serviceButtonsPane);
        VBox.setVgrow(textPane, Priority.ALWAYS); // 文本域布局部分自适应程序宽高
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea,Priority.ALWAYS);
    }

    /**
     * 设置服务按钮列表
     */
    private void setFlowPane(FlowPane flowPane) {
        ObservableList<Node> children = flowPane.getChildren();
        children.clear(); // 清除所有的按钮组件
        System.out.println("查询文件目录::" + path);
        List<Map<String, FileEntity>> fileList = FileUtil.getFileList(path);
        for (Map<String,FileEntity> map : fileList) {
            for (String str : map.keySet()) {
                FileEntity entity = map.get(str);
                Button button = new Button(str.toUpperCase());
                button.setStyle("-fx-background-color: green");
                entity.setOpen(false);
                String port = entity.getPort();
                if (port != null && !"".equals(port)) {
                    try {
                        Process process = Runtime.getRuntime().exec("netstat -ano"); // 根据端口号查看进程是否存在
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
                    } catch (IOException e) {
                        e.printStackTrace();
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
                children.add(button);
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
        boolean isOpen = entity.isOpen();
        String serverName = entity.getServerName();
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
                                String pid = line.split("\\s+")[5];
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
                    String[] cdRoom = new String[] {"cmd","/C","cd /d " + entity.getParentPath() + " && java -jar -Dspring.profiles.active=" + serverName + " common-data-2.0.6.jar"};
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
                    Process process = Runtime.getRuntime().exec("lsof -i :" + port);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                        if (line.contains("LISTEN")) {
                            String[] parts = line.trim().split("\\s+");
                            String pid = parts[1];
                            Process killProcess = Runtime.getRuntime().exec("kill " + pid);
                            killProcess.waitFor();
                            System.out.println("Process " + pid + " has been killed");
                            String cn = "\"" + serverName.toUpperCase() + "\"服务停止成功！！！";
                            textArea.setText(cn);
                            break;
                        }
                    }
                    reader.close();
                    button.setStyle("-fx-background-color: green;");
                    entity.setOpen(false);
                } else {
                    String[] cdRoom = new String[]{"/bin/sh","-c","cd " + entity.getParentPath() + " && java -jar -Dspring.profiles.active=" + serverName + " common-data-2.0.6.jar"};
                    Process process = Runtime.getRuntime().exec(cdRoom);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                        textArea.setText(sb.toString());
                    }
                    BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder errSB = new StringBuilder();
                    String errLine;
                    while ((errLine = errReader.readLine()) != null) {
                        errSB.append(errLine).append("\n");
                        textArea.setText(errSB.toString());
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

    public void addClick(ActionEvent actionEvent) throws IOException {
        System.out.println("点击了\"新增\"菜单 == " + actionEvent);
        Stage stage = new Stage();
        VBox box = new VBox();
        box.setStyle("-fx-padding: 10");
        ObservableList<Node> children = box.getChildren();
        GridPane textPane = new GridPane();
        TextArea area = new TextArea();
        URL url = this.getClass().getClassLoader().getResource("template/application.properties");
        BufferedInputStream bis = null;
        if (url != null) {
            bis = new BufferedInputStream(Files.newInputStream(Paths.get(url.getPath())));
        }
        StringBuilder sb = new StringBuilder();
        int data;
        if (bis != null) {
            while ((data = bis.read()) != -1) {
                sb.append((char) data);
            }
        }
        String body = sb.toString();
        String content = UnicodeBackslashU.unicodeToCn(body);
        area.setText(content);
        textPane.add(area,1,1);
        children.add(textPane);
        GridPane buttonPane = new GridPane();
        buttonPane.setStyle("-fx-alignment: center;-fx-hgap: 50;");
        Button closeBtn = new Button("取消");
        closeBtn.setOnMouseClicked(mouseEvent -> stage.close());
        Button saveBtn = new Button("保存");
        saveBtn.setOnMouseClicked(mouseEvent -> {
            ObservableList<Node> child = serviceButtonsPane.getChildren();
            Button btn = new Button(RandomChinese.getSingleChinese(2));
            btn.setStyle("-fx-background-color: green;");
            child.add(btn);
            stage.close();
        });
        buttonPane.add(closeBtn,1,1);
        buttonPane.add(saveBtn,2,1);
        children.add(buttonPane);
        VBox.setVgrow(textPane,Priority.ALWAYS);
        GridPane.setVgrow(area,Priority.ALWAYS);
        GridPane.setHgrow(area,Priority.ALWAYS);
        VBox.setVgrow(buttonPane,Priority.ALWAYS);
        Scene scene = new Scene(box, 600, 400);
        scene.getStylesheets().add("css/main.css");
        // 设置Stage的样式为无边框
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.setTitle("新增服务");
        stage.getIcons().add(new Image("images/logo.png"));
        stage.show();
    }

    public void exitClick() {
        Platform.exit();
    }

    public void configClick(ActionEvent actionEvent) {
        System.out.println("点击了\"系统配置\"菜单 == " + actionEvent);
        Stage stage = new Stage();
        VBox box = new VBox();
        box.setStyle("-fx-padding: 10");
        ObservableList<Node> children = box.getChildren();
        GridPane textPane = new GridPane();
        TextArea area = new TextArea();
        textPane.add(area,1,1);
        children.add(textPane);
        GridPane buttonPane = new GridPane();
        buttonPane.setStyle("-fx-alignment: center;-fx-hgap: 50;");
        Button closeBtn = new Button("取消");
        closeBtn.setOnMouseClicked(mouseEvent -> stage.close());
        Button saveBtn = new Button("保存");
        saveBtn.setOnMouseClicked(mouseEvent -> {
            path = area.getText();
            setFlowPane(serviceButtonsPane);
            stage.close();
        });
        buttonPane.add(closeBtn,1,1);
        buttonPane.add(saveBtn,2,1);
        children.add(buttonPane);
        VBox.setVgrow(textPane,Priority.ALWAYS);
        GridPane.setVgrow(area,Priority.ALWAYS);
        GridPane.setHgrow(area,Priority.ALWAYS);
        VBox.setVgrow(buttonPane,Priority.ALWAYS);
        Scene scene = new Scene(box, 600, 200);
        scene.getStylesheets().add("css/main.css");
        // 设置Stage的样式为无边框
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.setTitle("系统配置");
        stage.getIcons().add(new Image("images/logo.png"));
        stage.show();
    }

    public void aboutClick() {
        AlertUtil.aboutAlert();
    }

}
