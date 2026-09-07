package org.example.rouge;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // 加载 FXML 界面
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("hello-view.fxml"));
        Parent root = fxmlLoader.load();
        // 创建 Scene
        Scene scene = new Scene(root, 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        // 👇 关闭窗口前的确认弹窗
        stage.setOnCloseRequest(event -> {
            // 创建确认对话框
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认退出");
            alert.setHeaderText("你确定要退出程序吗？");
            alert.setContentText("如果还有未保存的内容，请先保存。");
            // 显示对话框，并等待用户选择
            Optional<ButtonType> result = alert.showAndWait();
            // 如果用户点击的不等于“确定”，就取消关闭
            if (result.isPresent() && result.get() != ButtonType.OK) {
                event.consume(); // 阻止窗口关闭
            }
            // 如果点击的是“确定”，自动放行，窗口正常关闭
        });
        // 显示窗口
        stage.show();
    }
}
