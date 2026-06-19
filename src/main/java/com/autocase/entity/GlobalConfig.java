package com.autocase.entity;

import javafx.beans.property.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局配置实体 - 存储应用级别的配置
 */
public class GlobalConfig {

    // 用例格式配置
    private String caseFormat; // json, xml, yaml, excel
    private String caseFormatExtension; // .json, .xml, .yaml, .xlsx

    // 测试脚本语言配置
    private List<ScriptLanguageConfig> scriptLanguages;

    // 自定义热键配置
    private List<HotkeyConfig> hotkeys;

    public GlobalConfig() {
        this.caseFormat = "json";
        this.caseFormatExtension = ".json";
        this.scriptLanguages = new ArrayList<>();
        // 默认支持的语言
        scriptLanguages.add(new ScriptLanguageConfig("python", "py", "python", true));
        scriptLanguages.add(new ScriptLanguageConfig("java", "java", "java", true));
        scriptLanguages.add(new ScriptLanguageConfig("cpp", "cpp", "g++", true));
        scriptLanguages.add(new ScriptLanguageConfig("csharp", "cs", "dotnet", true));
        scriptLanguages.add(new ScriptLanguageConfig("gdscript", "gd", "godot", true));

        // 默认热键
        this.hotkeys = new ArrayList<>();
        hotkeys.add(new HotkeyConfig("运行测试", "F5"));
        hotkeys.add(new HotkeyConfig("运行全部测试", "F6"));
        hotkeys.add(new HotkeyConfig("停止测试", "F7"));
        hotkeys.add(new HotkeyConfig("刷新用例", "F9"));
        hotkeys.add(new HotkeyConfig("新建用例", "CTRL+N"));
        hotkeys.add(new HotkeyConfig("保存结果", "CTRL+S"));
    }

    public String getCaseFormat() {
        return caseFormat;
    }

    public void setCaseFormat(String caseFormat) {
        this.caseFormat = caseFormat;
        switch (caseFormat) {
            case "xml":
                this.caseFormatExtension = ".xml";
                break;
            case "yaml":
                this.caseFormatExtension = ".yaml";
                break;
            case "excel":
                this.caseFormatExtension = ".xlsx";
                break;
            default:
                this.caseFormatExtension = ".json";
        }
    }

    public String getCaseFormatExtension() {
        return caseFormatExtension;
    }

    public List<ScriptLanguageConfig> getScriptLanguages() {
        return scriptLanguages;
    }

    public void setScriptLanguages(List<ScriptLanguageConfig> scriptLanguages) {
        this.scriptLanguages = scriptLanguages;
    }

    // ==================== 热键配置 ====================

    public List<HotkeyConfig> getHotkeys() {
        return hotkeys;
    }

    public void setHotkeys(List<HotkeyConfig> hotkeys) {
        this.hotkeys = hotkeys;
    }

    /**
     * 脚本语言配置（JavaFX 属性支持）
     */
    public static class ScriptLanguageConfig {
        private final StringProperty name;
        private final StringProperty extension;
        private final StringProperty command;
        private final BooleanProperty enabled;

        public ScriptLanguageConfig() {
            this.name = new SimpleStringProperty();
            this.extension = new SimpleStringProperty();
            this.command = new SimpleStringProperty();
            this.enabled = new SimpleBooleanProperty(true);
        }

        public ScriptLanguageConfig(String name, String extension, String command, boolean enabled) {
            this.name = new SimpleStringProperty(name);
            this.extension = new SimpleStringProperty(extension);
            this.command = new SimpleStringProperty(command);
            this.enabled = new SimpleBooleanProperty(enabled);
        }

        public String getName() {
            return name.get();
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public StringProperty nameProperty() {
            return name;
        }

        public String getExtension() {
            return extension.get();
        }

        public void setExtension(String extension) {
            this.extension.set(extension);
        }

        public StringProperty extensionProperty() {
            return extension;
        }

        public String getCommand() {
            return command.get();
        }

        public void setCommand(String command) {
            this.command.set(command);
        }

        public StringProperty commandProperty() {
            return command;
        }

        public boolean isEnabled() {
            return enabled.get();
        }

        public void setEnabled(boolean enabled) {
            this.enabled.set(enabled);
        }

        public BooleanProperty enabledProperty() {
            return enabled;
        }
    }

    /**
     * 热键配置
     */
    public static class HotkeyConfig {
        private String actionName;
        private String keyCombination;

        public HotkeyConfig() {
            this.actionName = "";
            this.keyCombination = "";
        }

        public HotkeyConfig(String actionName, String keyCombination) {
            this.actionName = actionName;
            this.keyCombination = keyCombination;
        }

        public String getActionName() {
            return actionName;
        }

        public void setActionName(String actionName) {
            this.actionName = actionName;
        }

        public String getKeyCombination() {
            return keyCombination;
        }

        public void setKeyCombination(String keyCombination) {
            this.keyCombination = keyCombination;
        }
    }
}
