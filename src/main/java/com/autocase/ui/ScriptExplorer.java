package com.autocase.ui;

import com.autocase.util.Constants;
import com.autocase.util.DialogUtil;
import com.autocase.util.FileUtil;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 脚本资源管理器 - 自动化测试面板专用，显示脚本目录树
 */
public class ScriptExplorer extends TreeView<String> {

    private String rootDirectory;

    // 剪贴板数据
    private String clipboardPath;
    private boolean clipboardIsCut = false;

    // 文件系统监听
    private WatchService watchService;
    private ExecutorService watchExecutor;
    private volatile boolean watching = false;

    public ScriptExplorer() {
        setPrefWidth(250);
        setCellFactory(tv -> new ScriptTreeCell());
        setupContextMenu();
        // 双击打开文本查看器
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                TreeItem<String> selected = getSelectionModel().getSelectedItem();
                if (selected != null && new File(selected.getValue()).isFile()) {
                    TextViewerDialog viewer = new TextViewerDialog(selected.getValue());
                    viewer.show();
                }
            }
        });
    }

    /**
     * 设置右键菜单
     */
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem createDirItem = new MenuItem("新建目录");
        MenuItem viewTextItem = new MenuItem("查看文本");
        MenuItem renameItem = new MenuItem("重命名");
        MenuItem copyItem = new MenuItem("复制");
        MenuItem cutItem = new MenuItem("剪切");
        MenuItem pasteItem = new MenuItem("粘贴");
        MenuItem deleteItem = new MenuItem("删除");
        MenuItem expandAllItem = new MenuItem("完全展开");
        MenuItem collapseAllItem = new MenuItem("完全折叠");
        MenuItem refreshItem = new MenuItem("刷新");

        contextMenu.getItems().addAll(
                createDirItem,
                new SeparatorMenuItem(),
                viewTextItem,
                new SeparatorMenuItem(),
                renameItem, copyItem, cutItem, pasteItem, deleteItem,
                new SeparatorMenuItem(),
                expandAllItem, collapseAllItem, refreshItem
        );

        // 根据选中项类型启用/禁用菜单项
        contextMenu.setOnShowing(e -> {
            TreeItem<String> selected = getSelectionModel().getSelectedItem();
            boolean hasSelection = selected != null;
            boolean isDirectory = hasSelection && new File(selected.getValue()).isDirectory();
            boolean isFile = hasSelection && new File(selected.getValue()).isFile();
            boolean hasClipboard = clipboardPath != null;

            createDirItem.setDisable(!isDirectory);
            viewTextItem.setDisable(!isFile);
            renameItem.setDisable(!hasSelection);
            copyItem.setDisable(!hasSelection);
            cutItem.setDisable(!hasSelection);
            pasteItem.setDisable(!hasClipboard || !isDirectory);
            deleteItem.setDisable(!hasSelection);
            expandAllItem.setDisable(!isDirectory);
            collapseAllItem.setDisable(!isDirectory);
        });

        // 新建目录
        createDirItem.setOnAction(e -> {
            TreeItem<String> selected = getSelectionModel().getSelectedItem();
            if (selected != null && new File(selected.getValue()).isDirectory()) {
                DialogUtil.showInputDialog("新建目录", "请输入目录名称:", "新目录", name -> {
                    if (name != null && !name.trim().isEmpty()) {
                        File parentDir = new File(selected.getValue());
                        if (FileUtil.createDirectory(parentDir, name)) {
                            refreshTree();
                        } else {
                            DialogUtil.showError("创建目录失败");
                        }
                    }
                });
            }
        });

        // 查看文本
        viewTextItem.setOnAction(e -> {
            TreeItem<String> selected = getSelectionModel().getSelectedItem();
            if (selected != null && new File(selected.getValue()).isFile()) {
                TextViewerDialog viewer = new TextViewerDialog(selected.getValue());
                viewer.show();
            }
        });

        // 重命名
        renameItem.setOnAction(e -> {
            TreeItem<String> selected = getSelectionModel().getSelectedItem();
            if (selected != null) {
                File file = new File(selected.getValue());
                String currentName = file.getName();
                DialogUtil.showInputDialog("重命名", "请输入新名称:", currentName, newName -> {
                    if (FileUtil.renameFile(file, newName)) {
                        refreshTree();
                    } else {
                        DialogUtil.showError("重命名失败");
                    }
                });
            }
        });

        // 复制
        copyItem.setOnAction(e -> {
            TreeItem<String> selected = getSelectionModel().getSelectedItem();
            if (selected != null) {
                clipboardPath = selected.getValue();
                clipboardIsCut = false;
            }
        });

        // 剪切
        cutItem.setOnAction(e -> {
            TreeItem<String> selected = getSelectionModel().getSelectedItem();
            if (selected != null) {
                clipboardPath = selected.getValue();
                clipboardIsCut = true;
            }
        });

        // 粘贴
        pasteItem.setOnAction(e -> {
            TreeItem<String> selected = getSelectionModel().getSelectedItem();
            if (selected != null && clipboardPath != null && new File(selected.getValue()).isDirectory()) {
                File source = new File(clipboardPath);
                File targetDir = new File(selected.getValue());
                File target = new File(targetDir, source.getName());

                boolean success;
                if (source.isDirectory()) {
                    success = FileUtil.copyDirectory(source, target);
                    if (success && clipboardIsCut) {
                        FileUtil.deleteDirectory(source);
                        clipboardPath = null;
                    }
                } else {
                    try {
                        Files.copy(source.toPath(), target.toPath());
                        success = true;
                        if (clipboardIsCut) {
                            source.delete();
                            clipboardPath = null;
                        }
                    } catch (IOException ex) {
                        success = false;
                        DialogUtil.showError("粘贴失败: " + ex.getMessage());
                    }
                }

                if (success) {
                    refreshTree();
                } else if (!source.isDirectory()) {
                    DialogUtil.showError("粘贴失败");
                }
            }
        });

        // 删除
        deleteItem.setOnAction(e -> {
            TreeItem<String> selected = getSelectionModel().getSelectedItem();
            if (selected != null) {
                File file = new File(selected.getValue());
                String itemName = file.getName();
                String content = file.isDirectory() ? "目录及其所有内容将被删除" : "文件将被删除";

                if (DialogUtil.showConfirm("确认删除", "确定要删除 " + itemName + " ?", content)) {
                    boolean success = file.isDirectory()
                            ? FileUtil.deleteDirectory(file)
                            : file.delete();
                    if (success) {
                        refreshTree();
                    } else {
                        DialogUtil.showError("删除失败");
                    }
                }
            }
        });

        // 完全展开
        expandAllItem.setOnAction(e -> {
            TreeItem<String> selected = getSelectionModel().getSelectedItem();
            if (selected != null) {
                expandAll(selected);
            }
        });

        // 完全折叠
        collapseAllItem.setOnAction(e -> {
            TreeItem<String> selected = getSelectionModel().getSelectedItem();
            if (selected != null) {
                collapseAll(selected);
            }
        });

        // 刷新
        refreshItem.setOnAction(e -> refreshTree());

        setContextMenu(contextMenu);
    }

    /**
     * 递归展开所有子节点
     */
    private void expandAll(TreeItem<String> item) {
        if (item == null) return;
        item.setExpanded(true);
        if (item.getChildren().isEmpty() && new File(item.getValue()).isDirectory()) {
            loadChildren(item, new File(item.getValue()));
        }
        for (TreeItem<String> child : item.getChildren()) {
            expandAll(child);
        }
    }

    /**
     * 递归折叠所有子节点
     */
    private void collapseAll(TreeItem<String> item) {
        if (item == null) return;
        item.setExpanded(false);
        for (TreeItem<String> child : item.getChildren()) {
            collapseAll(child);
        }
    }

    /**
     * 获取选中的文件路径
     */
    public String getSelectedFilePath() {
        TreeItem<String> selected = getSelectionModel().getSelectedItem();
        if (selected != null && new File(selected.getValue()).isFile()) {
            return selected.getValue();
        }
        return null;
    }

    /**
     * 设置根目录并刷新树
     */
    public void setRootDirectory(String directoryPath) {
        stopWatching();
        this.rootDirectory = directoryPath;
        refreshTree();
        startWatching();
    }

    /**
     * 刷新目录树
     */
    private void refreshTree() {
        if (rootDirectory == null || rootDirectory.isEmpty()) {
            setRoot(null);
            return;
        }

        File rootFile = new File(rootDirectory);
        if (!rootFile.exists() || !rootFile.isDirectory()) {
            setRoot(null);
            return;
        }

        TreeItem<String> rootItem = buildTree(rootFile);
        setRoot(rootItem);
        rootItem.setExpanded(true);
    }

    /**
     * 递归构建目录树
     */
    private TreeItem<String> buildTree(File directory) {
        TreeItem<String> item = new TreeItem<>(directory.getAbsolutePath());
        item.setExpanded(false);
        loadChildren(item, directory);
        return item;
    }

    /**
     * 加载目录的子节点
     */
    private void loadChildren(TreeItem<String> parentItem, File directory) {
        parentItem.getChildren().clear();

        File[] subDirs = directory.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                parentItem.getChildren().add(buildTree(subDir));
            }
        }

        File[] files = directory.listFiles(File::isFile);
        if (files != null) {
            for (File file : files) {
                parentItem.getChildren().add(new TreeItem<>(file.getAbsolutePath()));
            }
        }
    }

    /**
     * 启动文件系统监听
     */
    private void startWatching() {
        if (rootDirectory == null || watching) {
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path rootPath = Paths.get(rootDirectory);
            registerDirectoryRecursive(rootPath);

            watchExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ScriptExplorer-WatchService");
                t.setDaemon(true);
                return t;
            });

            watching = true;
            watchExecutor.submit(this::watchLoop);
        } catch (IOException e) {
            System.err.println("Failed to start file watcher: " + e.getMessage());
        }
    }

    /**
     * 递归注册目录监听
     */
    private void registerDirectoryRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path child : stream) {
                    if (Files.isDirectory(child)) {
                        registerDirectoryRecursive(child);
                    }
                }
            }
        }
    }

    /**
     * 监听循环
     */
    private void watchLoop() {
        while (watching) {
            try {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    Platform.runLater(() -> refreshTree());
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 停止监听
     */
    private void stopWatching() {
        watching = false;
        if (watchExecutor != null) {
            watchExecutor.shutdownNow();
            watchExecutor = null;
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                // ignore
            }
            watchService = null;
        }
    }

    /**
     * 展开到指定路径并选中
     */
    public void expandToPath(String filePath) {
        if (filePath == null || getRoot() == null) {
            return;
        }

        File targetFile = new File(filePath);
        String targetPath = targetFile.getAbsolutePath();

        TreeItem<String> current = getRoot();
        String currentPath = current.getValue();

        if (currentPath.equals(targetPath)) {
            current.setExpanded(true);
            getSelectionModel().select(current);
            return;
        }

        while (current != null) {
            current.setExpanded(true);

            if (current.getChildren().isEmpty()) {
                File currentDir = new File(current.getValue());
                loadChildren(current, currentDir);
            }

            TreeItem<String> found = null;
            for (TreeItem<String> child : current.getChildren()) {
                if (child.getValue().equals(targetPath)) {
                    found = child;
                    break;
                }
                if (targetPath.startsWith(child.getValue() + File.separator)) {
                    found = child;
                    break;
                }
            }

            if (found == null) {
                break;
            }

            current = found;

            if (current.getValue().equals(targetPath)) {
                getSelectionModel().select(current);
                int index = getRow(current);
                if (index >= 0) {
                    scrollTo(index);
                }
                break;
            }
        }
    }

    /**
     * 树单元格渲染 - 根据文件类型显示对应图标
     */
    private static class ScriptTreeCell extends TreeCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                File file = new File(item);
                String displayName = file.getName();
                if (displayName.isEmpty()) {
                    displayName = item;
                }
                boolean isDirectory = file.isDirectory();
                boolean isScriptFile = com.autocase.util.FileUtil.isScriptFile(file);
                boolean dirHasScriptFiles = isDirectory && com.autocase.util.FileUtil.containsScriptFiles(file);

                HBox box = new HBox(5);
                SVGPath icon;
                if (isDirectory) {
                    icon = createFolderIcon();
                } else {
                    icon = createFileIconByExtension(file.getName());
                }
                box.getChildren().add(icon);

                Text text = new Text(displayName);
                if (isScriptFile) {
                    text.setFill(Color.web(Constants.COLOR_SCRIPT_FILE));
                } else if (dirHasScriptFiles) {
                    text.setFill(Color.web(Constants.COLOR_DIR_WITH_SCRIPTS));
                } else if (isDirectory) {
                    text.setFill(Color.web(Constants.COLOR_FOLDER));
                } else {
                    text.setFill(Color.web(Constants.COLOR_NORMAL_FILE));
                }
                box.getChildren().add(text);
                setGraphic(box);
                setText(null);

                // 选中时文字改为白色
                if (isSelected()) {
                    text.setFill(Color.WHITE);
                }
            }
        }

        private SVGPath createFolderIcon() {
            SVGPath icon = new SVGPath();
            icon.setContent("M2 6a2 2 0 012-2h5l2 2h9a2 2 0 012 2v10a2 2 0 01-2 2H4a2 2 0 01-2-2V6z");
            icon.setFill(Color.web(Constants.COLOR_FOLDER));
            icon.setScaleX(0.8);
            icon.setScaleY(0.8);
            return icon;
        }

        /**
         * 根据文件扩展名返回对应的SVG图标
         */
        private static SVGPath createFileIconByExtension(String fileName) {
            String ext = getExtension(fileName).toLowerCase();
            return switch (ext) {
                case "java" -> createJavaIcon();
                case "py" -> createPythonIcon();
                case "json", "jsonl" -> createJsonIcon();
                case "xml", "html", "xhtml", "htm", "svg" -> createXmlHtmlIcon();
                case "js", "ts", "mjs", "cjs" -> createJsIcon();
                case "md", "markdown", "mdown" -> createMarkdownIcon();
                case "yml", "yaml" -> createYamlIcon();
                case "properties", "cfg", "ini", "conf", "env" -> createConfigIcon();
                case "csv", "xls", "xlsx" -> createSpreadsheetIcon();
                case "sql" -> createSqlIcon();
                case "sh", "bat", "cmd", "ps1" -> createScriptIcon();
                case "gd", "tscn", "tresn", "tres" -> createGodotIcon();
                default -> createDefaultFileIcon();
            };
        }

        private static String getExtension(String fileName) {
            int dotIndex = fileName.lastIndexOf('.');
            return dotIndex > 0 ? fileName.substring(dotIndex + 1) : "";
        }

        // --- Java 咖啡杯图标 ---
        private static SVGPath createJavaIcon() {
            SVGPath svg = new SVGPath();
            svg.setContent(
                "M7 2C4.8 2 3 3.8 3 6v7c0 3.9 3.1 7 7 7s7-3.1 7-7V6c0-2.2-1.8-4-4-4H7z" +
                "M5 6c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2v7c0 2.8-2.2 5-5 5s-5-2.2-5-5V6z" +
                "M17 9h2c1.1 0 2 .9 2 2v1c0 1.7-1.3 3-3 3v-6z"
            );
            svg.setFill(Color.web("#F89820"));
            svg.setScaleX(0.85); svg.setScaleY(0.85);
            return svg;
        }

        // --- Python 图标 ---
        private static SVGPath createPythonIcon() {
            SVGPath svg = new SVGPath();
            svg.setContent(
                "M12 2C6.48 2 2 4.02 2 7v2c0 2.34 2.18 4.22 5.09 4.86C7.03 14.02 6 15.4 6 17v3c0 2.76 2.69 5 6 5s6-2.24 6-5v-3c0-1.6-1.03-2.98-2.09-4.14C18.82 13.22 21 11.34 21 9V7c0-2.98-4.48-5-9-5z" +
                "M12 4c3.87 0 7 1.35 7 3v2c0 1.65-3.13 3-7 3S5 10.65 5 9V7c0-1.65 3.13-3 7-3z" +
                "M12 11c-2.76 0-5 .9-5 2v3c0 1.1 2.24 2 5 2s5-.9 5-2v-3c0-1.1-2.24-2-5-2z"
            );
            svg.setFill(Color.web("#3776AB"));
            svg.setScaleX(0.85); svg.setScaleY(0.85);
            return svg;
        }

        // --- JSON 图标 ---
        private static SVGPath createJsonIcon() {
            SVGPath svg = new SVGPath();
            svg.setContent(
                "M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 2l5 5h-5V4zM6 20V4h5v7h7v9H6z" +
                "M9 12h1v4H9zm3 0h1v4h-1z"
            );
            svg.setFill(Color.web("#CB4A32"));
            svg.setScaleX(0.8); svg.setScaleY(0.8);
            return svg;
        }

        // --- XML/HTML 图标 ---
        private static SVGPath createXmlHtmlIcon() {
            SVGPath svg = new SVGPath();
            svg.setContent(
                "M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 2l5 5h-5V4zM6 20V4h5v7h7v9H6z" +
                "M9 11l-2 2 2 2m4-4l2 2-2 2"
            );
            svg.setFill(Color.web("#E34C26"));
            svg.setScaleX(0.8); svg.setScaleY(0.8);
            return svg;
        }

        // --- JS/TS 图标 ---
        private static SVGPath createJsIcon() {
            SVGPath svg = new SVGPath();
            svg.setContent(
                "M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 2l5 5h-5V4zM6 20V4h5v7h7v9H6z" +
                "M9 11h5v1H9zm0 2h4v1H9zm0 2h3v1H9z"
            );
            svg.setFill(Color.web("#F7DF1E"));
            svg.setScaleX(0.8); svg.setScaleY(0.8);
            return svg;
        }

        // --- Markdown 图标 ---
        private static SVGPath createMarkdownIcon() {
            SVGPath svg = new SVGPath();
            svg.setContent(
                "M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 2l5 5h-5V4zM6 20V4h5v7h7v9H6z" +
                "M9 11h5M11.5 13.5L9 16m0-5l2.5 2.5"
            );
            svg.setFill(Color.web("#083FA1"));
            svg.setScaleX(0.8); svg.setScaleY(0.8);
            return svg;
        }

        // --- YAML 图标 ---
        private static SVGPath createYamlIcon() {
            SVGPath svg = new SVGPath();
            svg.setContent(
                "M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 2l5 5h-5V4zM6 20V4h5v7h7v9H6z" +
                "M9 11h1v4H9zm3-4h1v4h-1z"
            );
            svg.setFill(Color.web("#CB171E"));
            svg.setScaleX(0.8); svg.setScaleY(0.8);
            return svg;
        }

        // --- 配置文件图标 ---
        private static SVGPath createConfigIcon() {
            SVGPath svg = new SVGPath();
            svg.setContent(
                "M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 2l5 5h-5V4zM6 20V4h5v7h7v9H6z" +
                "M9 11h1v1H9zm3 0h1v1h-1zM9 14h1v1H9zm3 0h1v1h-1z"
            );
            svg.setFill(Color.web("#6D8086"));
            svg.setScaleX(0.8); svg.setScaleY(0.8);
            return svg;
        }

        // --- 表格图标 ---
        private static SVGPath createSpreadsheetIcon() {
            SVGPath svg = new SVGPath();
            svg.setContent(
                "M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 2l5 5h-5V4zM6 20V4h5v7h7v9H6z" +
                "M9 10h5v1H9zm0 3h5v1H9zm0 3h5v1H9z"
            );
            svg.setFill(Color.web("#207245"));
            svg.setScaleX(0.8); svg.setScaleY(0.8);
            return svg;
        }

        // --- SQL 图标 ---
        private static SVGPath createSqlIcon() {
            SVGPath svg = new SVGPath();
            svg.setContent(
                "M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 2l5 5h-5V4zM6 20V4h5v7h7v9H6z" +
                "M9 11c0-.6.4-1 1-1s1 .4 1 1-.4 1-1 1-1-.4-1-1zm3 2c0-.6.4-1 1-1s1 .4 1 1-.4 1-1 1-1-.4-1-1z"
            );
            svg.setFill(Color.web("#336791"));
            svg.setScaleX(0.8); svg.setScaleY(0.8);
            return svg;
        }

        // --- Shell 脚本图标 ---
        private static SVGPath createScriptIcon() {
            SVGPath svg = new SVGPath();
            svg.setContent(
                "M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 2l5 5h-5V4zM6 20V4h5v7h7v9H6z" +
                "M9 12l1.5 1.5L13 11"
            );
            svg.setFill(Color.web("#4EAA25"));
            svg.setScaleX(0.8); svg.setScaleY(0.8);
            return svg;
        }

        // --- Godot 文件图标 ---
        private static SVGPath createGodotIcon() {
            SVGPath svg = new SVGPath();
            svg.setContent(
                "M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 2l5 5h-5V4zM6 20V4h5v7h7v9H6z" +
                "M10 11a2 2 0 100 4 2 2 0 000-4z"
            );
            svg.setFill(Color.web("#478CBF"));
            svg.setScaleX(0.8); svg.setScaleY(0.8);
            return svg;
        }

        // --- 默认文件图标 ---
        private static SVGPath createDefaultFileIcon() {
            SVGPath svg = new SVGPath();
            svg.setContent("M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 2l5 5h-5V4zM6 20V4h5v7h7v9H6z");
            svg.setFill(Color.web(Constants.COLOR_FILE));
            svg.setScaleX(0.8); svg.setScaleY(0.8);
            return svg;
        }
    }
}
