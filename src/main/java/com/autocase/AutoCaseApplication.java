package com.autocase;

import com.autocase.ui.MainWindow;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

/**
 * 应用程序入口
 * 使用JFXPanel初始化JavaFX工具包，支持java -jar方式运行
 */
public class AutoCaseApplication {

    public static void main(String[] args) {
        // 强制修复控制台编码，解决Windows下GBK导致的中文乱码
        fixConsoleEncoding();

        // 使用JFXPanel触发JavaFX工具包初始化（不需要模块系统）
        new JFXPanel();
        
        Platform.runLater(() -> {
            Stage primaryStage = new Stage();
            MainWindow mainWindow = new MainWindow(primaryStage);
            mainWindow.show();
        });
    }

    /**
     * 强制将System.out和System.err的编码重置为UTF-8
     * System.setProperty在JDK中对已初始化的PrintStream不生效，
     * 必须直接替换PrintStream才能彻底解决Windows控制台乱码
     */
    private static void fixConsoleEncoding() {
        try {
            // 设置系统属性（对新创建的流有效）
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("stdout.encoding", "UTF-8");
            System.setProperty("stderr.encoding", "UTF-8");
            System.setProperty("sun.jnu.encoding", "UTF-8");

            // 关键：直接替换stdout和stderr为UTF-8编码的PrintStream
            // 这样所有后续的System.out/err.println都会用UTF-8输出
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8.name()));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            // 理论上不可能发生，UTF-8必定支持
            System.err.println("[警告] 无法设置UTF-8编码: " + e.getMessage());
        }
    }
}
