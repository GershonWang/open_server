package com.dongpl.controller;

import com.dongpl.entity.FileEntity;
import com.dongpl.utils.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
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
    @FXML
    public TextArea textArea;
    @FXML
    public Menu changeServiceMenu;
    @FXML
    public Menu deleteServiceMenu;
    @FXML
    public Menu viewServiceLogMenu;
    public static String path = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + "data";

    @Override
    public void initialize(URL location, ResourceBundle resource) {
        setServiceMenu(); // 刷新服务模块相关
        menuBar.prefWidthProperty().bind(box.widthProperty()); //菜单栏宽度绑定为box宽度
        serviceButtonsPane.prefWidthProperty().bind(box.widthProperty()); //flowPane宽度绑定为box宽度
        VBox.setVgrow(textPane, Priority.ALWAYS); // 文本域布局部分自适应程序宽高
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea,Priority.ALWAYS);
    }

    /**
     * 设置服务按钮列表
     */
    private void setServiceButtonPane(FlowPane serviceButtonsPane) {
        ObservableList<Node> children = serviceButtonsPane.getChildren();
        children.clear(); // 清除所有的按钮组件
        System.out.println("查询文件目录::" + path);
        FileUtil.createJar(path); //创建服务目录并校验填充服务程序
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
                                    button.setStyle("-fx-background-color: #f56c6c;");
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
            AlertUtil.errAlert("当前服务端口号为空无法加载，请重新修改配置文件！！！");
            return;
        }
        long startTime = entity.getStartTime();
        long nowTime = System.currentTimeMillis();
        if (startTime != 0 && nowTime - startTime <= 10000) {
            AlertUtil.errAlert("当前服务正在启动中，请稍后停止！！！");
            return;
        }
        String serverName = entity.getServerName();
        try {
            String systemOS = System.getProperty("os.name");
            if (systemOS.toLowerCase().contains("windows")) {
                if (entity.isOpen()) {
                    System.out.println(systemOS + "系统，关闭\"" + serverName.toUpperCase() + "\"服务...");
                    Process process = Runtime.getRuntime().exec("netstat -ano"); // netstat -ano | findstr 11397  获取系统进程列表 tasklist
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("GBK")));
                    String line;
                    Pattern pattern = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+");
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            String partStr = matcher.group().split(":")[1];
                            if (Objects.equals(partStr, port)) {
                                String status = line.split("\\s+")[4]; // 获取活动状态
                                String pid = line.split("\\s+")[5]; // 获取进程号
                                if ("LISTENING".equals(status) && !"0".equals(pid)) {
                                    //根据进程id杀死进程
                                    Process killProcess = Runtime.getRuntime().exec("taskkill /F /PID " + pid);
                                    killProcess.waitFor();
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
                                        button.setStyle("-fx-background-color: #f56c6c;");
                                        entity.setOpen(true);
                                    }
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
                                    return;
                                }
                            } else {
                                textArea.setText("未发现\"" + serverName.toUpperCase() + "\"服务在运行！！！");
                                button.setStyle("-fx-background-color: green;");
                                entity.setOpen(false);
                            }
                        }
                    }
                } else {
                    System.out.println(systemOS + "系统，开启\"" + serverName.toUpperCase() + "\"服务...");
                    textArea.setText("\"" + serverName.toUpperCase() + "\"服务启动中，喝杯茶等待十秒钟吧~~~");
                    String[] cdRoom = new String[] {"cmd","/C","cd /d " + entity.getParentPath() + " && java -jar -Dspring.profiles.active=" + serverName + " open_server-1.0-SNAPSHOT.jar"};
                    // 开启两个线程
                    new Thread(() -> {
                        boolean isSuccess = true;
                        try {
                            String logPath = path + File.separator + "log";
                            File file = new File(logPath);
                            if (!file.exists()) {
                                if (file.mkdirs()) {
                                    System.out.println("日志写入成功！");
                                } else {
                                    System.out.println("日志写入失败！");
                                    return;
                                }
                            }
                            File outputFile = new File(logPath + File.separator + "application-" + serverName + ".log");
                            ProcessBuilder pb = new ProcessBuilder(cdRoom);
                            pb.redirectErrorStream(true);
                            pb.redirectOutput(outputFile);
                            pb.start();
                            entity.setStartTime(System.currentTimeMillis());
                            Thread.sleep(10000);
                            BufferedReader reader = new BufferedReader(new FileReader(outputFile));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.contains("Application run failed")) {
                                    isSuccess = false;
                                    break;
                                }
                            }
                            if (isSuccess) {
                                textArea.setText("\"" + serverName.toUpperCase() + "\"服务启动成功！！！");
                                button.setStyle("-fx-background-color: #f56c6c;");
                                entity.setOpen(true);
                            } else {
                                textArea.setText("\"" + serverName.toUpperCase() + "\"服务启动失败！！！");
                                button.setStyle("-fx-background-color: green;");
                                entity.setOpen(false);
                            }
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }).start();
                }
            } else if (systemOS.toLowerCase().contains("linux")){
                if (entity.isOpen()) {
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
                    String[] cdRoom = new String[]{"/bin/sh","-c","cd " + entity.getParentPath() + " && java -jar -Dspring.profiles.active=" + serverName + " open_server-1.0-SNAPSHOT.jar"};
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
                    button.setStyle("-fx-background-color: #f56c6c;");
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
            textArea.setText(UnicodeBackslashU.unicodeToCn(sb.toString()));
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public void addClick() throws IOException {
        InputStream inputStream = Objects.requireNonNull(getClass().getResourceAsStream("/template/application.properties"));
        serviceConfigFile(inputStream,"新增服务");
        setServiceMenu(); // 刷新服务模块相关
    }

    public void exitClick() {
        Platform.exit();
    }

    public void configClick() {
        Stage stage = new Stage();
        VBox box = new VBox();
        box.setStyle("-fx-padding: 10");
        ObservableList<Node> children = box.getChildren();
        Label label = new Label("设置缓存配置文件存放路径:");
        label.setStyle("-fx-text-fill: #fbedde;-fx-opacity: 0.7;-fx-font-weight: bold;-fx-font-size: 18px");
        children.add(label);
        GridPane serviceConfigPane = new GridPane();
        TextField serviceConfigAddress = new TextField();
        serviceConfigAddress.setText(path); // 默认回显默认路径
        Button chooseBtn = new Button("选择目录");
        chooseBtn.setOnMouseClicked(mouseEvent -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(stage);
            if (selectedDirectory != null) {
                serviceConfigAddress.setText(selectedDirectory.getAbsolutePath());
            }
        });
        serviceConfigPane.add(serviceConfigAddress,1,1);
        serviceConfigPane.add(chooseBtn,2,1);
        GridPane.setMargin(chooseBtn,new Insets(0,0,0,20));
        children.add(serviceConfigPane);
        Label nacLabel = new Label("设置Nacos注册中心服务地址：");
        nacLabel.setStyle("-fx-text-fill: #fbedde;-fx-opacity: 0.7;-fx-font-weight: bold;-fx-font-size: 18px");
        children.add(nacLabel);
        GridPane nacosConfigPane = new GridPane();
        TextField nacosConfigAddress = new TextField();
        String bootstrapProp = path + File.separator + "bootstrap.properties";
        try {
            File file = new File(bootstrapProp);
            if (file.exists()) {
                InputStream inputStream = Files.newInputStream(Paths.get(bootstrapProp));
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("spring.cloud.nacos.config.server-addr=")) {
                        nacosConfigAddress.setText(line.split("=")[1]);
                        break;
                    }
                }
                inputStream.close();
            } else {
                FileUtil.copyFile("template/bootstrap.properties",bootstrapProp);
                nacosConfigAddress.setText("26.196.211.120:8848");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        nacosConfigPane.add(nacosConfigAddress,1,1);
        children.add(nacosConfigPane);
        GridPane buttonPane = new GridPane();
        buttonPane.setStyle("-fx-alignment: center;-fx-pref-height: 50px");
        Button closeBtn = new Button("取消");
        closeBtn.setOnMouseClicked(mouseEvent -> stage.close());
        Button saveBtn = new Button("保存");
        saveBtn.setOnMouseClicked(mouseEvent -> {
            // 获取配置路径并修改全局变量
            String text = serviceConfigAddress.getText();
            if (text != null && !"".equals(text)) {
                path = text;
            } else {
                AlertUtil.errAlert("缓存配置文件路径不能为空");
                return;
            }
            // 刷新服务模块及相关配置文件
            setServiceMenu();
            // 获取nacos地址并修改配置文件
            String addressText = nacosConfigAddress.getText();
            if (addressText != null && !"".equals(addressText)) {
                try {
                    String newBootstrapProp = path + File.separator + "bootstrap.properties";
                    InputStream inputStream = Files.newInputStream(Paths.get(newBootstrapProp));
                    BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("spring.cloud.nacos.config.server-addr=")) {
                            sb.append("spring.cloud.nacos.config.server-addr=").append(addressText).append("\n");
                        } else {
                            sb.append(line).append("\n");
                        }
                    }
                    inputStream.close();
                    FileWriter fileWriter = new FileWriter(bootstrapProp);
                    fileWriter.write(UnicodeBackslashU.cnToUnicode(sb.toString()));
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                AlertUtil.errAlert("nacos注册地址不能为空");
                return;
            }
            stage.close();
            textArea.setText("系统配置修改成功!");
        });
        buttonPane.add(closeBtn,1,1);
        buttonPane.add(saveBtn,2,1);
        children.add(buttonPane);
        VBox.setVgrow(serviceConfigPane,Priority.ALWAYS);
        GridPane.setHgrow(serviceConfigAddress,Priority.ALWAYS);
        GridPane.setVgrow(chooseBtn,Priority.ALWAYS);
        VBox.setVgrow(nacosConfigPane,Priority.ALWAYS);
        GridPane.setVgrow(nacosConfigAddress,Priority.ALWAYS);
        GridPane.setHgrow(nacosConfigAddress,Priority.ALWAYS);
        GridPane.setMargin(closeBtn,new Insets(0,40,0,0));
        Scene scene = new Scene(box, 600, 200);
        scene.getStylesheets().add("css/main.css");
        stage.setResizable(false); // 禁用最大化按钮
        stage.setScene(scene);
        stage.setTitle("系统配置");
        stage.getIcons().add(new Image("images/logo.png"));
        stage.show();
    }

    public void aboutClick() {
        AlertUtil.aboutAlert();
    }

    private void setServiceMenu() {
        // 加载修改菜单
        setChangeServiceMenu(changeServiceMenu);
        // 加载删除菜单
        setDeleteServiceMenu(deleteServiceMenu);
        // 加载查看日志菜单
        setViewServiceLogMenu(viewServiceLogMenu);
        // 加载服务按钮布局
        setServiceButtonPane(serviceButtonsPane);
    }

    private void setChangeServiceMenu(Menu changeServiceMenu) {
        ObservableList<MenuItem> items = changeServiceMenu.getItems();
        items.clear();
        List<String> serverNames = FileUtil.getServerNames(path);
        for (String serverName : serverNames) {
            MenuItem menuItem = new MenuItem(serverName.toUpperCase());
            menuItem.setOnAction(actionEvent -> {
                try {
                    InputStream inputStream = Files.newInputStream(Paths.get(path + File.separator + "application-" + serverName + ".properties"));
                    serviceConfigFile(inputStream,"修改服务");
                    setServiceMenu(); // 刷新服务模块相关
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            items.add(menuItem);
        }
    }

    private void setDeleteServiceMenu(Menu deleteServiceMenu) {
        ObservableList<MenuItem> items = deleteServiceMenu.getItems();
        items.clear();
        List<String> serverNames = FileUtil.getServerNames(path);
        for (String serverName : serverNames) {
            MenuItem menuItem = new MenuItem(serverName.toUpperCase());
            menuItem.setOnAction(actionEvent -> {
                Stage stage = new Stage();
                VBox box = new VBox();
                box.setStyle("-fx-padding: 10");
                ObservableList<Node> children = box.getChildren();
                GridPane textPane = new GridPane();
                textPane.setStyle("-fx-alignment: center");
                Label label = new Label("是否确认删除\"" + serverName.toUpperCase() + "\"服务");
                label.setStyle("-fx-text-fill: #fbedde;-fx-opacity: 0.7;-fx-font-size: 18px;-fx-font-weight: bold");
                textPane.add(label,1,1);
                children.add(textPane);
                GridPane buttonPane = new GridPane();
                buttonPane.setStyle("-fx-alignment: center;-fx-pref-height: 50px");
                Button closeBtn = new Button("取消");
                closeBtn.setOnMouseClicked(mouseEvent -> stage.close());
                Button saveBtn = new Button("确认");
                saveBtn.setOnMouseClicked(mouseEvent -> {
                    File file = new File(path + File.separator + "application-" + serverName + ".properties");
                    if (file.delete()) {
                        textArea.setText("\"" + serverName.toUpperCase() + "\"服务删除成功！");
                    } else {
                        textArea.setText("\"" + serverName.toUpperCase() + "\"服务删除失败！");
                    }
                    String logPath = path + File.separator + "log" + File.separator + "application-" + serverName + ".log";
                    File logFile = new File(logPath);
                    if (logFile.exists() && logFile.delete()) {
                        System.out.println("日志文件删除成功！");
                    }
                    stage.close();
                    setServiceMenu(); // 刷新服务模块相关
                });
                buttonPane.add(closeBtn,1,1);
                buttonPane.add(saveBtn,2,1);
                children.add(buttonPane);
                VBox.setVgrow(textPane,Priority.ALWAYS);
                GridPane.setMargin(closeBtn,new Insets(0,40,0,0));
                Scene scene = new Scene(box, 300, 150);
                scene.getStylesheets().add("css/main.css");
                stage.setScene(scene);
                stage.setTitle("警告");
                stage.getIcons().add(new Image("images/logo.png"));
                stage.show();
            });
            items.add(menuItem);
        }
    }

    private void setViewServiceLogMenu(Menu viewServiceLogMenu) {
        ObservableList<MenuItem> items = viewServiceLogMenu.getItems();
        items.clear();
        List<String> serverNames = FileUtil.getServerNames(path);
        for (String serverName : serverNames) {
            MenuItem menuItem = new MenuItem(serverName.toUpperCase());
            menuItem.setOnAction(actionEvent -> {
                try {
                    String logPath = path + File.separator + "log" + File.separator + "application-" + serverName + ".log";
                    File file = new File(logPath);
                    if (!file.exists()) {
                        AlertUtil.errAlert("\"" + serverName.toUpperCase() + "\"服务暂无日志可供查看");
                        return;
                    }
                    Stage stage = new Stage();
                    VBox box = new VBox();
                    box.setStyle("-fx-padding: 10");
                    ObservableList<Node> children = box.getChildren();
                    GridPane gridPane = new GridPane();
                    gridPane.setPrefHeight(35.0);
                    GridPane textPane = new GridPane();
                    TextArea area = new TextArea();
                    area.setEditable(false); // 设置为不可编辑模式
                    Label label = new Label();
                    label.setText("日志文件：");
                    label.setStyle("-fx-text-fill: #fbedde;-fx-opacity: 0.7;-fx-font-weight: bold;-fx-font-size: 18px");
                    gridPane.add(label,1,1);
                    ComboBox<Integer> comboBox = new ComboBox<>();
                    comboBox.getItems().addAll(100,200,500,1000);
                    comboBox.getSelectionModel().select(0);
                    comboBox.valueProperty().addListener(((observable, oldVal, newVal) -> {
                        try {
                            readLastLines(area,logPath,newVal);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }));
                    gridPane.add(comboBox,2,1);
                    GridPane.setMargin(comboBox,new Insets(0,10,0,0));
                    Button button = new Button();
                    button.setText("开启滚动日志");
                    AtomicReference<Timer> timer = new AtomicReference<>();
                    button.setOnMouseClicked(mouseEvent -> {
                        String text = button.getText();
                        if (text.startsWith("开启")) {
                            System.out.println("开启滚动日志");
                            timer.set(new Timer());
                            TimerTask task = new TimerTask(){
                                @Override
                                public void run() {
                                    try {
                                        Integer selectedItem = comboBox.getSelectionModel().getSelectedItem();
                                        readLastLines(area,logPath,selectedItem);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            };
                            timer.get().schedule(task,3000,3000);
                            button.setText("关闭滚动日志");
                        } else {
                            System.out.println("关闭滚动日志");
                            timer.get().cancel(); //移除定时任务
                            timer.get().purge();
                            button.setText("开启滚动日志");
                        }
                    });
                    gridPane.add(button,3,1);
                    children.add(gridPane);
                    Integer selectedItem = comboBox.getSelectionModel().getSelectedItem();
                    readLastLines(area,logPath,selectedItem);
                    textPane.add(area,1,1);
                    children.add(textPane);
                    VBox.setVgrow(textPane,Priority.ALWAYS);
                    GridPane.setVgrow(area,Priority.ALWAYS);
                    GridPane.setHgrow(area,Priority.ALWAYS);
                    Scene scene = new Scene(box, 1200, 800);
                    scene.getStylesheets().add("css/main.css");
                    stage.setScene(scene);
                    stage.getIcons().add(new Image("images/logo.png"));
                    stage.setTitle("查看\"" + serverName.toUpperCase() + "\"服务日志");
                    stage.show();
                    stage.setOnCloseRequest(windowEvent -> {
//                        windowEvent.consume(); //阻止默认的窗口关闭行为
                        if (timer.get() != null) {
                            System.out.println("关闭窗口同时关闭滚动日志");
                            timer.get().cancel(); // 移除定时任务
                            timer.get().purge();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            items.add(menuItem);
        }
    }

    private void readLastLines(TextArea area,String logPath,Integer number) throws IOException {
        Deque<String> lines = new ArrayDeque<>();
        InputStream inputStream = Files.newInputStream(Paths.get(logPath));
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("GB2312")));
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line);
            if (lines.size() > number) {
                lines.removeFirst();
            }
        }
        Platform.runLater(() -> {
            area.clear();
            for (String str : lines) {
                area.appendText(str + "\n");
                area.positionCaret(area.getText().length()); // 将光标移动到文本最后面
//                area.setScrollTop(Double.MAX_VALUE);
                area.scrollTopProperty().set(area.getLength()); // 自动滚动到最底部
            }
        });
    }

    private void serviceConfigFile(InputStream inputStream, String configType) throws IOException {
        Stage stage = new Stage();
        VBox box = new VBox();
        box.setStyle("-fx-padding: 10");
        ObservableList<Node> children = box.getChildren();
        GridPane gridPane = new GridPane();
        gridPane.setPrefHeight(35.0);
        Label label = new Label();
        label.setText("新增服务".equals(configType) ? "配置文件模板：" : "配置文件：");
        label.setStyle("-fx-text-fill: #fbedde;-fx-opacity: 0.7;-fx-font-weight: bold;-fx-font-size: 18px");
        gridPane.add(label,1,1);
        children.add(gridPane);
        GridPane textPane = new GridPane();
        TextArea area = new TextArea();
        StringBuilder sb = new StringBuilder();
        String servName = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
            if (line.startsWith("spring.application.name")) {
                String s = line.split("=")[1];
                servName = s.split("-")[1];
            }
        }
        inputStream.close();
        area.setText(UnicodeBackslashU.unicodeToCn(sb.toString()));
        textPane.add(area,1,1);
        children.add(textPane);
        GridPane buttonPane = new GridPane();
        buttonPane.setStyle("-fx-alignment: center;-fx-pref-height: 50px;");
        Button closeBtn = new Button("取消");
        closeBtn.setOnMouseClicked(mouseEvent -> {
            stage.close();
            deleteTemplateProperties(); // 删除临时模板配置文件
        });
        Button saveBtn = new Button("保存");
        String finalServName = servName;
        saveBtn.setOnMouseClicked(mouseEvent -> {
            String templatePath = path + File.separator + "template.properties";
            try {
                FileWriter fileWriter = new FileWriter(templatePath);
                fileWriter.write(area.getText());
                fileWriter.close();
                System.out.println("临时文件写入成功");
            } catch (IOException e) {
                e.printStackTrace();
            }
            String port = PropertiesUtil.getType(templatePath,"server.port");
            if (port == null || "".equals(port)) {
                AlertUtil.errAlert("新增服务".equals(configType) ? "服务端口不能为空，新增服务失败！请重新编辑配置文件" : "服务端口不能为空，修改服务失败！请重新编辑配置文件");
                return;
            }
            String modelName = PropertiesUtil.getType(templatePath,"spring.application.name");
            String[] split = modelName.split("-");
            if (split.length != 3) {
                AlertUtil.errAlert("新增服务".equals(configType) ? "服务名称格式错误(必须为：abc-xxx-data 形式)，\n新增服务失败！请重新编辑配置文件" : "服务名称格式错误(必须为：abc-xxx-data 形式)，\n修改服务失败！请重新编辑配置文件");
                return;
            }
            String serverName = split[1];
            if (serverName == null || "".equals(serverName)) {
                AlertUtil.errAlert("新增服务".equals(configType) ? "服务名称不能为空，新增服务失败！请重新编辑配置文件" : "服务名称不能为空，修改服务失败！请重新编辑配置文件");
                return;
            }
            if ("修改服务".equals(configType)) {
                // 删除原来的配置文件
                File file = new File(path + File.separator + "application-" + finalServName + ".properties");
                if (file.delete()) {
                    System.out.println("原配置文件删除成功！");
                } else {
                    System.out.println("原配置文件删除失败！");
                }
            }
            List<String> serverPorts = FileUtil.getServerPorts(path);
            if (serverPorts.contains(port)) {
                AlertUtil.errAlert("新增服务".equals(configType) ? "服务端口已存在，新增服务失败！请重新编辑配置文件" : "服务端口已存在，修改服务失败！请重新编辑配置文件");
                return;
            }
            List<String> serverNames = FileUtil.getServerNames(path);
            if (serverNames.contains(serverName)) {
                AlertUtil.errAlert("新增服务".equals(configType) ? "服务名称已存在，新增服务失败！请重新编辑配置文件" : "服务名称已存在，修改服务失败！请重新编辑配置文件");
                return;
            }
            try {
                FileWriter fileWriter = new FileWriter(path + File.separator + "application-" + serverName + ".properties");
                fileWriter.write(UnicodeBackslashU.cnToUnicode(area.getText()));
                fileWriter.close();
                System.out.println("文件写入成功");
            } catch (IOException e) {
                e.printStackTrace();
            }
            deleteTemplateProperties(); // 删除临时缓存文件
            setServiceMenu(); // 刷新服务模块相关
            stage.close();
            textArea.setText("新增服务".equals(configType) ? "新增\"" + serverName.toUpperCase() + "\"服务成功！" : "修改\"" + serverName.toUpperCase() + "\"服务成功！");
        });
        buttonPane.add(closeBtn,1,1);
        buttonPane.add(saveBtn,2,1);
        children.add(buttonPane);
        VBox.setVgrow(textPane,Priority.ALWAYS);
        GridPane.setVgrow(area,Priority.ALWAYS);
        GridPane.setHgrow(area,Priority.ALWAYS);
        GridPane.setMargin(closeBtn,new Insets(0,40,0,0));
        Scene scene = new Scene(box, 1200, 800);
        scene.getStylesheets().add("css/main.css");
        stage.setScene(scene);
        stage.setTitle("新增服务".equals(configType) ? "新增服务" : "修改服务");
        stage.getIcons().add(new Image("images/logo.png"));
        stage.show();
        stage.setOnCloseRequest(windowEvent -> {
            System.out.println("关闭窗口同时删除缓存文件");
            deleteTemplateProperties();
        });
    }

    private void deleteTemplateProperties() {
        String templatePath = path + File.separator + "template.properties";
        File file = new File(templatePath);
        if (file.exists() && file.delete()) {
            System.out.println("缓存文件删除成功！");
        }
    }

}
