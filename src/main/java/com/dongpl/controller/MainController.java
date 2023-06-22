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
