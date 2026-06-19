package com.autocase.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 对话框工具类
 */
public class DialogUtil {

    private DialogUtil() {
        // 工具类不允许实例化
    }

    /**
     * 显示错误提示
     */
    public static void showError(String message) {
        showError(null, message);
    }

    /**
     * 显示错误提示（指定所有者窗口）
     */
    public static void showError(Window owner, String message) {
        runLaterOrNow(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            if (owner != null) {
                alert.initOwner(owner);
            }
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * 显示信息提示
     */
    public static void showInfo(String message) {
        showInfo(null, message);
    }

    /**
     * 显示信息提示（指定所有者窗口）
     */
    public static void showInfo(Window owner, String message) {
        runLaterOrNow(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            if (owner != null) {
                alert.initOwner(owner);
            }
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * 显示信息提示（自定义标题）
     */
    public static void showInfoWithTitle(String title, String message) {
        showInfoWithTitle(null, title, message);
    }

    /**
     * 显示信息提示（指定所有者窗口和自定义标题）
     */
    public static void showInfoWithTitle(Window owner, String title, String message) {
        runLaterOrNow(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            if (owner != null) {
                alert.initOwner(owner);
            }
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * 显示警告提示
     */
    public static void showWarning(String title, String message) {
        showWarning(null, title, message);
    }

    /**
     * 显示警告提示（指定所有者窗口）
     */
    public static void showWarning(Window owner, String title, String message) {
        runLaterOrNow(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            if (owner != null) {
                alert.initOwner(owner);
            }
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * 显示确认对话框
     */
    public static boolean showConfirm(String title, String header, String content) {
        return showConfirm(null, title, header, content);
    }

    /**
     * 显示确认对话框（指定所有者窗口）
     */
    public static boolean showConfirm(Window owner, String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * 显示输入对话框
     */
    public static void showInputDialog(String title, String message, String defaultValue, Consumer<String> callback) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);

        dialog.showAndWait().ifPresent(callback);
    }

    /**
     * 在JavaFX线程上执行，如果已在JavaFX线程则直接执行
     */
    private static void runLaterOrNow(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    /**
     * 修复JavaFX ListView已知bug：空列表时点击导致IndexOutOfBoundsException
     * 原因：ListViewBehavior在鼠标按下或获得焦点时调用clearAndSelect(0)，但空列表size=0导致越界
     * 方案：设置全局异常处理器吞掉此特定异常（JavaFX框架层面的bug无法从应用层完全规避）
     */
    public static <T> void fixListViewEmptyClickBug(javafx.scene.control.ListView<T> listView) {
        // 监听items变化，空列表时禁用（减少触发概率）
        listView.getItems().addListener((javafx.collections.ListChangeListener<T>) change -> {
            listView.setDisable(listView.getItems().isEmpty());
        });
        if (listView.getItems().isEmpty()) {
            listView.setDisable(true);
        }
        // 注册全局异常处理器作为最终兜底
        installIndexOOBHandler();
    }

    /**
     * 修复JavaFX TreeView已知bug：切换根节点（清空后重建）时点击导致IndexOutOfBoundsException
     * 原因：TreeView的MultipleSelectionModel在root为null时调用clearAndSelect(0)越界
     * 方案：root为null时禁用TreeView + 全局异常处理器兜底
     */
    public static <T> void fixTreeViewEmptyClickBug(javafx.scene.control.TreeView<T> treeView) {
        treeView.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            treeView.setDisable(newRoot == null);
        });
        if (treeView.getRoot() == null) {
            treeView.setDisable(true);
        }
        // 全局异常处理器兜底
        installIndexOOBHandler();
    }

    /**
     * 安装全局异常处理器，专门吞掉JavaFX的IndexOutOfBoundsException bug。
     * 这是最终防线——当setDisable等预防措施都未能阻止时的最后保障。
     * 此异常不影响程序功能，只是JavaFX框架内部的边界检查缺陷。
     */
    private static volatile boolean oobHandlerInstalled = false;

    private static void installIndexOOBHandler() {
        if (oobHandlerInstalled) return;
        synchronized (DialogUtil.class) {
            if (oobHandlerInstalled) return;
            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
                if (e instanceof IndexOutOfBoundsException
                        && e.getMessage() != null
                        && e.getMessage().contains("size: 0")) {
                    // 静默忽略——这是JavaFX ListView/TreeView在空数据时的已知bug
                    return;
                }
                // 其他异常交给默认处理器
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(t, e);
            });
            oobHandlerInstalled = true;
        }
    }
}
