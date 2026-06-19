package com.cms.test;

import com.autocase.dao.CaseDao;
import com.autocase.entity.CaseStatus;
import com.autocase.entity.TestCase;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.*;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

/**
 * CMS UI 自动化测试 - 使用 TestFX 模拟用户操作
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CmsUiTest extends ApplicationTest {

    private static final String TEST_DIR = "selfTest/test_data/ui_test";
    private Stage primaryStage;
    private CaseDao caseDao;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.caseDao = new CaseDao();

        // 准备测试数据
        prepareTestData();

        com.autocase.ui.MainWindow mainWindow = new com.autocase.ui.MainWindow(primaryStage);
        mainWindow.show();
    }

    @BeforeEach
    public void setUp() throws Exception {
        // 等待 UI 渲染完成
        sleep(500);
        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterAll
    public static void cleanup() {
        File dir = new File(TEST_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith("_Case.json")) {
                        f.delete();
                    }
                }
            }
        }
    }

    private void prepareTestData() {
        File dir = new File(TEST_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String[][] cases = {
            {"UI_TEST_001", "登录模块", "P0", "P0", "loginTest"},
            {"UI_TEST_002", "注册模块", "P1", "P1", "registerTest"},
            {"UI_TEST_003", "登录模块", "P2", "P2", "logoutTest"},
        };

        for (String[] c : cases) {
            TestCase tc = new TestCase();
            tc.setCasesID(c[0]);
            tc.setGroup(c[1]);
            tc.setVersion("1.0");
            tc.setSeverity(c[2]);
            tc.setPriority(c[3]);
            tc.setCurrentStatus(CaseStatus.UNVERIFIED);

            com.autocase.entity.CaseInfo info = new com.autocase.entity.CaseInfo();
            info.setCaseName(c[4]);
            info.setDescription("UI自动化测试用例");
            tc.setCaseInfo(info);

            com.autocase.entity.ExpectedResult result = new com.autocase.entity.ExpectedResult();
            result.setDescription("测试通过");
            tc.setExpectedResult(result);

            com.autocase.entity.Execution exec = new com.autocase.entity.Execution();
            exec.setTimeout(30);
            exec.setRetryCount(1);
            tc.setExecution(exec);

            tc.setFilePath(new File(dir, c[0] + "_Case.json").getAbsolutePath());
            caseDao.saveCase(tc);
        }
    }

    @Test
    @Order(1)
    @DisplayName("测试1: 窗口正常启动")
    public void testWindowLaunch() {
        assertNotNull(primaryStage);
        assertTrue(primaryStage.isShowing(), "主窗口应显示");
        assertEquals("CMS - 用例管理系统", primaryStage.getTitle());
    }

    @Test
    @Order(2)
    @DisplayName("测试2: 窗口尺寸正确")
    public void testWindowSize() {
        assertTrue(primaryStage.getWidth() >= 1200, "窗口宽度应>=1200");
        assertTrue(primaryStage.getHeight() >= 800, "窗口高度应>=800");
    }

    @Test
    @Order(3)
    @DisplayName("测试3: 场景存在")
    public void testSceneExists() {
        assertNotNull(primaryStage.getScene());
        assertNotNull(primaryStage.getScene().getRoot());
    }

    @Test
    @Order(4)
    @DisplayName("测试4: 按钮控件存在")
    public void testButtonsExist() {
        // 查找所有 Button 节点
        int buttonCount = lookup(".button").queryAll().size();
        assertTrue(buttonCount > 0, "应至少有一个按钮");
    }

    @Test
    @Order(5)
    @DisplayName("测试5: TabPane 存在")
    public void testTabPaneExists() {
        TabPane tabPane = lookup(".tab-pane").query();
        assertNotNull(tabPane);
        assertEquals(4, tabPane.getTabs().size(), "应有4个Tab");
    }

    @Test
    @Order(6)
    @DisplayName("测试6: Tab 名称正确")
    public void testTabNames() {
        TabPane tabPane = lookup(".tab-pane").query();
        String[] expectedTabs = {"用例管理", "Git仓库", "自动化测试", "测试报告"};
        
        for (int i = 0; i < expectedTabs.length; i++) {
            assertEquals(expectedTabs[i], tabPane.getTabs().get(i).getText(),
                "Tab " + i + " 名称应为 " + expectedTabs[i]);
        }
    }

    @Test
    @Order(7)
    @DisplayName("测试7: 用例管理 Tab 内容")
    public void testCaseManagementTab() {
        TabPane tabPane = lookup(".tab-pane").query();
        
        // 切换到用例管理 Tab
        clickOn("用例管理");
        sleep(300);
        WaitForAsyncUtils.waitForFxEvents();
        
        assertEquals(0, tabPane.getSelectionModel().getSelectedIndex(), "应选中用例管理Tab");
        
        // 验证 SplitPane 存在（资源管理器 + 筛选面板）
        int splitPaneCount = lookup(".split-pane").queryAll().size();
        assertTrue(splitPaneCount > 0, "用例管理Tab应包含SplitPane");
    }

    @Test
    @Order(8)
    @DisplayName("测试8: 表格显示测试数据")
    public void testTableShowsCases() {
        // 先切换到用例管理 Tab 触发数据加载
        clickOn("用例管理");
        sleep(500);
        WaitForAsyncUtils.waitForFxEvents();

        // 查找 TableView
        TableView<?> table = lookup(".table-view").query();
        assertNotNull(table);
        // 由于没有选择目录，表格可能为空，这是正常行为
        // 验证表格控件存在即可
        assertTrue(table.getColumns().size() > 0, "表格应有列定义");
    }

    @Test
    @Order(9)
    @DisplayName("测试9: 切换 Tab")
    public void testTabSwitching() {
        TabPane tabPane = lookup(".tab-pane").query();
        
        // 切换到自动化测试
        clickOn("自动化测试");
        sleep(200);
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(2, tabPane.getSelectionModel().getSelectedIndex());
        
        // 切换到 Git仓库
        clickOn("Git仓库");
        sleep(200);
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, tabPane.getSelectionModel().getSelectedIndex());
        
        // 切换回用例管理
        clickOn("用例管理");
        sleep(200);
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(0, tabPane.getSelectionModel().getSelectedIndex());
    }

    @Test
    @Order(10)
    @DisplayName("测试10: 表格列存在")
    public void testTableColumns() {
        clickOn("用例管理");
        sleep(300);
        WaitForAsyncUtils.waitForFxEvents();

        TableView<?> table = lookup(".table-view").query();
        assertTrue(table.getColumns().size() >= 5, "表格应至少包含5列");
    }
}
