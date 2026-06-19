package com.autocase.ui;

import com.autocase.util.DialogUtil;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;

/**
 * 文本查看器/编辑器 - 支持查看和编辑文本文件，支持撤销/重做
 */
public class TextViewerDialog {

    private final String filePath;
    private final Stage dialog;
    private TextArea textArea;
    private Label statusLabel;

    // 撤销/重做栈
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private String lastSavedContent;

    // 可编辑的文本文件扩展名
    private static final String[] EDITABLE_EXTENSIONS = {
            ".txt", ".md", ".html", ".htm", ".css", ".js", ".ts",
            ".java", ".py", ".gd", ".cs", ".c", ".cpp", ".h", ".hpp",
            ".xml", ".json", ".yaml", ".yml", ".properties", ".ini",
            ".cfg", ".conf", ".log", ".sql", ".sh", ".bat", ".cmd",
            ".gradle", ".groovy", ".rb", ".go", ".rs", ".swift",
            ".kt", ".vue", ".jsx", ".tsx", ".scss", ".less", ".sass"
    };

    public TextViewerDialog(String filePath) {
        this.filePath = filePath;
        this.dialog = new Stage();
        buildUI();
    }

    private void buildUI() {
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("文本查看器 - " + Paths.get(filePath).getFileName().toString());
        dialog.setWidth(800);
        dialog.setHeight(600);

        BorderPane root = new BorderPane();

        // 文本编辑区
        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setFont(javafx.scene.text.Font.font("Consolas", 13));

        // 先创建状态栏（statusLabel 需要在 loadFileContent 之前初始化）
        HBox statusBar = createStatusBar();

        // 加载文件内容
        loadFileContent();

        // 监听文本变化，支持撤销/重做
        textArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!oldVal.equals(newVal)) {
                undoStack.push(oldVal);
                redoStack.clear();
                updateStatus();
            }
        });

        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        root.setCenter(scrollPane);

        root.setBottom(statusBar);

        Scene scene = new Scene(root);
        dialog.setScene(scene);

        // 快捷键（必须在 Scene 创建后设置）
        setupShortcuts();
    }

    private void loadFileContent() {
        try {
            Path path = Paths.get(filePath);
            byte[] bytes = Files.readAllBytes(path);
            String content = new String(bytes, StandardCharsets.UTF_8);

            // 检测是否为乱码（简单判断：如果包含大量替换字符，可能是二进制文件）
            long replacementCount = content.chars().filter(ch -> ch == '\uFFFD').count();
            if (replacementCount > content.length() * 0.1) {
                textArea.setText("[此文件可能为二进制文件，无法正确显示]\n\n" + content);
                textArea.setEditable(false);
            } else {
                textArea.setText(content);
                textArea.setEditable(isEditable());
            }

            lastSavedContent = content;
            updateStatus();
        } catch (IOException e) {
            textArea.setText("无法读取文件: " + e.getMessage());
            textArea.setEditable(false);
        }
    }

    private boolean isEditable() {
        String fileName = Paths.get(filePath).getFileName().toString().toLowerCase();
        for (String ext : EDITABLE_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(15);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #f0f0f0;");

        statusLabel = new Label();
        updateStatus();

        Button saveBtn = new Button("保存");
        saveBtn.setOnAction(e -> saveFile());

        Button undoBtn = new Button("撤销");
        undoBtn.setOnAction(e -> undo());

        Button redoBtn = new Button("重做");
        redoBtn.setOnAction(e -> redo());

        statusBar.getChildren().addAll(statusLabel, new Region(), undoBtn, redoBtn, saveBtn);
        HBox.setHgrow(new Region(), Priority.ALWAYS);

        return statusBar;
    }

    private void setupShortcuts() {
        Scene scene = dialog.getScene();

        // Ctrl+S 保存
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
                this::saveFile
        );

        // Ctrl+Z 撤销
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN),
                this::undo
        );

        // Ctrl+Y 重做
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN),
                this::redo
        );
    }

    private void saveFile() {
        if (!textArea.isEditable()) {
            return;
        }

        try {
            String content = textArea.getText();
            Files.write(Paths.get(filePath), content.getBytes(StandardCharsets.UTF_8));
            lastSavedContent = content;
            updateStatus();

            // 显示保存成功提示
            statusLabel.setText("已保存 | " + statusLabel.getText().split("\\|")[0].trim());
        } catch (IOException e) {
            DialogUtil.showError("保存失败: " + e.getMessage());
        }
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            String current = textArea.getText();
            redoStack.push(current);
            String previous = undoStack.pop();
            textArea.setText(previous);
            updateStatus();
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            String current = textArea.getText();
            undoStack.push(current);
            String next = redoStack.pop();
            textArea.setText(next);
            updateStatus();
        }
    }

    private void updateStatus() {
        String mode = textArea.isEditable() ? "可编辑" : "只读";
        int lines = textArea.getText().split("\n", -1).length;
        int chars = textArea.getText().length();
        int undoSize = undoStack.size();
        int redoSize = redoStack.size();

        statusLabel.setText(String.format("%s | %d 行, %d 字符 | 撤销:%d 重做:%d",
                mode, lines, chars, undoSize, redoSize));
    }

    public void show() {
        dialog.showAndWait();
    }
}
