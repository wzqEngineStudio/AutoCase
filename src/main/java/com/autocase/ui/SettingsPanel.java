package com.autocase.ui;

import com.autocase.dao.ConfigDao;
import com.autocase.entity.GlobalConfig;
import com.autocase.entity.GlobalConfig.HotkeyConfig;
import com.autocase.entity.GlobalConfig.ScriptLanguageConfig;
import com.autocase.util.DialogUtil;
import com.autocase.util.HashCache;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * 设置面板 - 全局配置界面
 */
public class SettingsPanel extends BorderPane {

    private final ConfigDao configDao;
    private GlobalConfig globalConfig;

    // 用例格式配置
    private ComboBox<String> caseFormatCombo;

    // 脚本语言配置
    private TableView<ScriptLanguageConfig> languageTable;
    private ObservableList<ScriptLanguageConfig> languageList;

    // 自定义命令
    private TextField customLangName;
    private TextField customLangExt;
    private TextField customLangCmd;

    // 热键配置
    private TableView<HotkeyConfig> hotkeyTable;
    private ObservableList<HotkeyConfig> hotkeyList;

    public SettingsPanel() {
        this.configDao = new ConfigDao();
        this.globalConfig = configDao.getGlobalConfig();
        buildUI();
        loadConfig();
    }

    private void buildUI() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f5f5f5;");

        // 用例格式设置
        VBox caseFormatSection = createCaseFormatSection();

        // 脚本语言设置
        VBox scriptLangSection = createScriptLanguageSection();

        // 热键设置
        VBox hotkeySection = createHotkeySection();

        // 开发者信息
        VBox aboutSection = createAboutSection();

        // 缓存管理
        VBox cacheSection = createCacheSection();

        // 保存按钮
        HBox buttonBox = createButtonBox();

        content.getChildren().addAll(caseFormatSection, scriptLangSection, hotkeySection, cacheSection, aboutSection, buttonBox);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        setCenter(scrollPane);
    }

    private VBox createCaseFormatSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        Label title = new Label("用例格式配置");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label desc = new Label("选择新建用例时使用的文件格式。已存在的其他格式用例仍可正常识别和编辑。");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #666;");

        HBox formatBox = new HBox(10);
        formatBox.setAlignment(Pos.CENTER_LEFT);

        Label formatLabel = new Label("默认格式:");
        caseFormatCombo = new ComboBox<>();
        caseFormatCombo.getItems().addAll("JSON (默认)", "XML", "YAML", "Excel");
        caseFormatCombo.setPrefWidth(200);

        formatBox.getChildren().addAll(formatLabel, caseFormatCombo);

        section.getChildren().addAll(title, desc, formatBox);
        return section;
    }

    private VBox createScriptLanguageSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        Label title = new Label("测试脚本语言配置");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label desc = new Label("配置支持的测试脚本语言及其执行命令。可启用/禁用语言或添加自定义命令。");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #666;");

        // 语言列表
        languageTable = new TableView<>();
        languageTable.setPrefHeight(200);
        languageTable.setEditable(true);

        TableColumn<ScriptLanguageConfig, Boolean> enabledCol = new TableColumn<>("启用");
        enabledCol.setPrefWidth(60);
        enabledCol.setCellValueFactory(cellData -> cellData.getValue().enabledProperty());
        enabledCol.setCellFactory(col -> new CheckBoxTableCell<>());

        TableColumn<ScriptLanguageConfig, String> nameCol = new TableColumn<>("语言名称");
        nameCol.setPrefWidth(120);
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setCellFactory(col -> new TextFieldTableCell<>());
        nameCol.setOnEditCommit(e -> e.getRowValue().setName(e.getNewValue()));

        TableColumn<ScriptLanguageConfig, String> extCol = new TableColumn<>("扩展名");
        extCol.setPrefWidth(80);
        extCol.setCellValueFactory(cellData -> cellData.getValue().extensionProperty());
        extCol.setCellFactory(col -> new TextFieldTableCell<>());
        extCol.setOnEditCommit(e -> e.getRowValue().setExtension(e.getNewValue()));

        TableColumn<ScriptLanguageConfig, String> cmdCol = new TableColumn<>("执行命令");
        cmdCol.setPrefWidth(200);
        cmdCol.setCellValueFactory(cellData -> cellData.getValue().commandProperty());
        cmdCol.setCellFactory(col -> new TextFieldTableCell<>());
        cmdCol.setOnEditCommit(e -> e.getRowValue().setCommand(e.getNewValue()));

        languageTable.getColumns().addAll(enabledCol, nameCol, extCol, cmdCol);

        // 添加自定义语言
        HBox addBox = new HBox(10);
        addBox.setAlignment(Pos.CENTER_LEFT);

        Label addLabel = new Label("添加自定义语言:");
        customLangName = new TextField();
        customLangName.setPromptText("名称 (如: ruby)");
        customLangName.setPrefWidth(120);

        customLangExt = new TextField();
        customLangExt.setPromptText("扩展名 (如: rb)");
        customLangExt.setPrefWidth(100);

        customLangCmd = new TextField();
        customLangCmd.setPromptText("执行命令 (如: ruby)");
        customLangCmd.setPrefWidth(120);

        Button addBtn = new Button("添加");
        addBtn.setOnAction(e -> addCustomLanguage());

        addBox.getChildren().addAll(addLabel, customLangName, customLangExt, customLangCmd, addBtn);

        section.getChildren().addAll(title, desc, languageTable, addBox);
        return section;
    }

    /**
     * 创建自定义热键配置板块
     */
    private VBox createHotkeySection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        Label title = new Label("自定义热键配置");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label desc = new Label("配置常用操作的快捷键。点击热键值可直接编辑，支持组合键（如 CTRL+SHIFT+R）。修改后需保存生效。");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #666;");

        // 热键列表
        hotkeyTable = new TableView<>();
        hotkeyTable.setPrefHeight(220);
        hotkeyTable.setEditable(true);

        TableColumn<HotkeyConfig, String> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(180);
        actionCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getActionName()));

        TableColumn<HotkeyConfig, String> keyCol = new TableColumn<>("快捷键");
        keyCol.setPrefWidth(200);
        keyCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getKeyCombination()));
        keyCol.setCellFactory(col -> new TextFieldTableCell<>());
        keyCol.setOnEditCommit(e -> e.getRowValue().setKeyCombination(e.getNewValue()));

        hotkeyTable.getColumns().addAll(actionCol, keyCol);

        // 提示信息
        Label tipLabel = new Label("💡 提示：支持 F1~F12、CTRL+字母、SHIFT+字母、ALT+字母、CTRL+SHIFT+字母 等组合。");
        tipLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        tipLabel.setWrapText(true);

        section.getChildren().addAll(title, desc, hotkeyTable, tipLabel);
        return section;
    }

    /**
     * 创建缓存管理板块
     */
    private VBox createCacheSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        Label title = new Label("⚡ 哈希缓存");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label desc = new Label("类似 Redis 的本地文件缓存，对数据源目录计算 MD5 哈希指纹。"
                + "当目录未变化时，启动时直接从缓存恢复用例和脚本列表，跳过全量文件扫描。");
        desc.setStyle("-fx-text-fill: #666; -fx-font-size: 12px; -fx-wrap-text: true;");
        desc.setMaxWidth(Double.MAX_VALUE);

        // 缓存状态显示
        TextArea cacheStatusArea = new TextArea();
        cacheStatusArea.setEditable(false);
        cacheStatusArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; "
                + "-fx-control-inner-background: #f8f8f8;");
        cacheStatusArea.setPrefRowCount(4);
        cacheStatusArea.setText(HashCache.getInstance().getStats());

        // 操作按钮
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_LEFT);

        Button refreshBtn = new Button("刷新状态");
        refreshBtn.setOnAction(e -> cacheStatusArea.setText(HashCache.getInstance().getStats()));

        Button clearBtn = new Button("清除全部缓存");
        clearBtn.setStyle("-fx-background-color: #ff5722; -fx-text-fill: white;");
        clearBtn.setOnAction(e -> {
            HashCache.getInstance().clearAll();
            cacheStatusArea.setText(HashCache.getInstance().getStats());
            DialogUtil.showInfo("已清除全部缓存，下次加载将重新扫描");
        });

        btnBox.getChildren().addAll(refreshBtn, clearBtn);

        section.getChildren().addAll(title, desc, cacheStatusArea, btnBox);
        return section;
    }

    /**
     * 创建开发者信息板块
     */
    private VBox createAboutSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); "
                + "-fx-background-radius: 8; "
                + "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.3), 8, 0, 0, 3);");

        Label title = new Label("👨‍💻 关于开发者");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: white;");

        Separator separator = new Separator();
        separator.setStyle("-fx-opacity: 0.3;");

        // 开发者信息卡片
        VBox infoCard = new VBox(8);
        infoCard.setPadding(new Insets(12));
        infoCard.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 6;");

        Label devNameLabel = new Label("开发者：wzqEngineStudio");
        devNameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label projectLabel = new Label("项目：AutoCase");
        projectLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 13px;");

        Label versionLabel = new Label("版本：v1.1.0");
        versionLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 12px;");

        Separator sep2 = new Separator();
        sep2.setStyle("-fx-opacity: 0.2;");

        Label contactTitle = new Label("📬 联系方式与支持");
        contactTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Hyperlink githubLink = new Hyperlink("🔗 GitHub 仓库地址");
        githubLink.setStyle("-fx-text-fill: #a3d9ff; -fx-font-size: 13px;");
        githubLink.setOnAction(e -> openUrl("https://github.com/wzqEngineStudio/AutoCase"));

        Label emailLabel = new Label("📧 邮箱：1275651410@qq.com");
        emailLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 12px;");

        Label issueHint = new Label("⚠️ 使用过程中如遇问题，可提交反馈！");
        issueHint.setWrapText(true);
        issueHint.setStyle("-fx-text-fill: #ffd93d; -fx-font-size: 12px; -fx-font-style: italic;");

        infoCard.getChildren().addAll(
                devNameLabel, projectLabel, versionLabel,
                sep2, contactTitle, githubLink, emailLabel, issueHint
        );

        section.getChildren().addAll(title, separator, infoCard);
        return section;
    }

    /**
     * 打开外部链接
     */
    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("无法打开链接");
            alert.setHeaderText(null);
            alert.setContentText("无法打开浏览器，请手动访问：\n" + url);
            alert.showAndWait();
        }
    }

    private HBox createButtonBox() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button("保存配置");
        saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        saveBtn.setOnAction(e -> saveConfig());

        Button resetBtn = new Button("恢复默认");
        resetBtn.setOnAction(e -> resetConfig());

        box.getChildren().addAll(resetBtn, saveBtn);
        return box;
    }

    private void loadConfig() {
        // 加载用例格式
        String format = globalConfig.getCaseFormat();
        switch (format) {
            case "xml":
                caseFormatCombo.setValue("XML");
                break;
            case "yaml":
                caseFormatCombo.setValue("YAML");
                break;
            case "excel":
                caseFormatCombo.setValue("Excel");
                break;
            default:
                caseFormatCombo.setValue("JSON (默认)");
        }

        // 加载脚本语言
        languageList = FXCollections.observableArrayList(globalConfig.getScriptLanguages());
        languageTable.setItems(languageList);

        // 加载热键配置
        hotkeyList = FXCollections.observableArrayList(globalConfig.getHotkeys());
        hotkeyTable.setItems(hotkeyList);
    }

    private void saveConfig() {
        // 保存用例格式
        String selectedFormat = caseFormatCombo.getValue();
        switch (selectedFormat) {
            case "XML":
                globalConfig.setCaseFormat("xml");
                break;
            case "YAML":
                globalConfig.setCaseFormat("yaml");
                break;
            case "Excel":
                globalConfig.setCaseFormat("excel");
                break;
            default:
                globalConfig.setCaseFormat("json");
        }

        // 保存脚本语言
        globalConfig.setScriptLanguages(new ArrayList<>(languageList));

        // 保存热键配置
        globalConfig.setHotkeys(new ArrayList<>(hotkeyList));

        // 持久化
        configDao.saveGlobalConfig(globalConfig);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("保存成功");
        alert.setHeaderText(null);
        alert.setContentText("配置已保存！热键将在下次启动时生效。");
        alert.showAndWait();
    }

    private void resetConfig() {
        globalConfig = new GlobalConfig();
        configDao.saveGlobalConfig(globalConfig);
        loadConfig();
    }

    private void addCustomLanguage() {
        String name = customLangName.getText().trim();
        String ext = customLangExt.getText().trim();
        String cmd = customLangCmd.getText().trim();

        if (name.isEmpty() || ext.isEmpty() || cmd.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("输入不完整");
            alert.setHeaderText(null);
            alert.setContentText("请填写完整的语言名称、扩展名和执行命令");
            alert.showAndWait();
            return;
        }

        ScriptLanguageConfig newLang = new ScriptLanguageConfig(name, ext, cmd, true);
        languageList.add(newLang);

        customLangName.clear();
        customLangExt.clear();
        customLangCmd.clear();
    }
}
