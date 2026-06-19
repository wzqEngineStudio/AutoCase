package com.autocase.ui;

import com.autocase.dao.ConfigDao;
import com.autocase.entity.GithubConfig;
import com.autocase.logic.GithubLogic;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Git仓库配置面板 - 包含设置和推送功能
 */
public class GithubPanel extends VBox {

    private final ConfigDao configDao;
    private final Stage primaryStage;
    private ConfigDao.CmsConfig config;
    private GithubConfig githubConfig;

    private final TextField urlField;
    private final TextField usernameField;
    private final PasswordField tokenField;
    private final Button pushBtn;
    private final Button saveBtn;
    private final Label statusLabel;

    public GithubPanel(ConfigDao configDao, Stage primaryStage) {
        this.configDao = configDao;
        this.primaryStage = primaryStage;
        this.config = configDao.loadConfig();
        this.githubConfig = config.getGithubConfig();

        urlField = new TextField();
        urlField.setPromptText("https://github.com/user/repo.git 或 https://gitee.com/user/repo.git");

        usernameField = new TextField();
        usernameField.setPromptText("用户名");

        tokenField = new PasswordField();
        tokenField.setPromptText("Personal Access Token");

        pushBtn = new Button("推送到仓库");
        saveBtn = new Button("保存配置");
        statusLabel = new Label();

        loadExistingConfig();
        buildUI();
    }

    private void loadExistingConfig() {
        if (githubConfig != null) {
            if (githubConfig.getRepositoryUrl() != null) {
                urlField.setText(githubConfig.getRepositoryUrl());
            }
            if (githubConfig.getUsername() != null) {
                usernameField.setText(githubConfig.getUsername());
            }
            // Token不回填，出于安全考虑
        }
    }

    private void buildUI() {
        setSpacing(15);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #fafafa;");

        // 标题
        Label titleLabel = new Label("Git 仓库配置");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // 提示
        Label hintLabel = new Label("支持 GitHub、Gitee、GitCode 等任意 Git 仓库。Token 在对应平台的开发者设置中生成");
        hintLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        hintLabel.setWrapText(true);

        // 表单区域
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        javafx.scene.layout.ColumnConstraints col0 = new javafx.scene.layout.ColumnConstraints();
        col0.setMinWidth(80);
        col0.setPrefWidth(80);
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        col1.setMinWidth(300);
        formGrid.getColumnConstraints().addAll(col0, col1);

        Label urlLabel = new Label("仓库URL:");
        Label usernameLabel = new Label("用户名:");
        Label tokenLabel = new Label("Token:");

        GridPane.setConstraints(urlLabel, 0, 0);
        GridPane.setConstraints(urlField, 1, 0);
        GridPane.setConstraints(usernameLabel, 0, 1);
        GridPane.setConstraints(usernameField, 1, 1);
        GridPane.setConstraints(tokenLabel, 0, 2);
        GridPane.setConstraints(tokenField, 1, 2);

        formGrid.getChildren().addAll(urlLabel, urlField, usernameLabel, usernameField, tokenLabel, tokenField);

        // 按钮区域
        HBox buttonBox = new HBox(10, saveBtn, pushBtn);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        // 状态标签
        statusLabel.setStyle("-fx-font-size: 12px;");
        statusLabel.setVisible(false);

        // 绑定事件
        saveBtn.setOnAction(e -> saveConfig());
        pushBtn.setOnAction(e -> pushToRepo());

        getChildren().addAll(titleLabel, hintLabel, formGrid, buttonBox, statusLabel);
    }

    private void saveConfig() {
        String url = urlField.getText().trim();
        String username = usernameField.getText().trim();
        String token = tokenField.getText().trim();

        if (url.isEmpty() || username.isEmpty() || token.isEmpty()) {
            showStatus("请填写所有字段", false);
            return;
        }

        if (!url.startsWith("https://") && !url.startsWith("git@")) {
            showStatus("仓库URL格式不正确", false);
            return;
        }

        GithubConfig newConfig = new GithubConfig(url, username, token);
        config.setGithubConfig(newConfig);
        configDao.saveConfig(config);
        githubConfig = newConfig;

        showStatus("配置已保存", true);
    }

    private void pushToRepo() {
        if (githubConfig == null || !githubConfig.isConfigured()) {
            showStatus("请先保存配置", false);
            return;
        }

        // 获取根目录（需要从外部传入，这里用回调）
        if (onPushRequested != null) {
            onPushRequested.run();
        }
    }

    private void showStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.setStyle(success
                ? "-fx-text-fill: green; -fx-font-size: 12px;"
                : "-fx-text-fill: red; -fx-font-size: 12px;");
        statusLabel.setVisible(true);
    }

    /**
     * 推送请求回调（由MainWindow提供根目录）
     */
    private Runnable onPushRequested;

    public void setOnPushRequested(Runnable handler) {
        this.onPushRequested = handler;
    }

    /**
     * 执行实际推送（由MainWindow调用）
     */
    public void doPush(String rootDirectory) {
        ProgressIndicator progress = new ProgressIndicator();
        progress.setProgress(-1);
        Label progressLabel = new Label("正在推送...");

        VBox progressBox = new VBox(10, progress, progressLabel);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(20));

        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.initOwner(primaryStage);
        progressStage.setTitle("推送中");
        progressStage.setScene(new Scene(progressBox, 200, 100));
        progressStage.setResizable(false);

        new Thread(() -> {
            GithubLogic logic = new GithubLogic(githubConfig);
            boolean success = logic.pushToGithub(rootDirectory, "Update test cases");

            Platform.runLater(() -> {
                progressStage.close();
                showStatus(success ? "推送成功" : "推送失败，请检查配置和网络", success);
            });
        }).start();

        progressStage.show();
    }

    /**
     * 创建GitHub Octocat图标按钮
     */
    public static Button createGithubButton() {
        Button btn = new Button();
        btn.setTooltip(new Tooltip("Git仓库管理"));

        // GitHub Octocat SVG路径
        SVGPath octocat = new SVGPath();
        octocat.setContent("M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12");
        octocat.setFill(Color.web("#333"));
        octocat.setScaleX(1.0);
        octocat.setScaleY(1.0);

        btn.setGraphic(octocat);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 5;");
        btn.setOnMouseEntered(e -> octocat.setFill(Color.web("#0366d6")));
        btn.setOnMouseExited(e -> octocat.setFill(Color.web("#333")));

        return btn;
    }
}
