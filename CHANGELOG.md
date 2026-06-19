# 更新日志 (Changelog)

所有 notable changes 都会记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [1.1.0] - 2025-06-20

### 🆕 新增功能

#### 用例管理
- **多格式用例支持** — 新建用例时可选 JSON / XML / YAML / Excel 格式，已存在的其他格式用例仍可正常识别和编辑
- **用例关联与互斥** — 支持定义用例间的关联关系（associatedCases）和互斥关系（mutuallyExclusiveCases），执行时自动识别依赖
- **用例状态机** — 完整的用例生命周期状态：UNVERIFIED → PASSED / FAILED_NOT_REGRESSED / BLOCKED / SKIPPED，自动化执行后自动回写状态

#### 自动化测试引擎
- **多语言脚本执行** — 内置 Python、Java、C/C++、C#、GDScript、JavaScript/TypeScript、Go、Rust、Ruby、Shell、Batch 共 12 种语言执行模板
- **并行执行** — 基于线程池的多脚本并行执行，自动按优先级（P0→P3）和步骤数排序调度
- **超时熔断** — 单脚本执行超时（默认 60s）自动强制终止，防止死循环阻塞
- **暂停/恢复/停止** — 执行过程中支持暂停、恢复和完全停止控制
- **用例-脚本自动关联** — 通过名称模糊匹配自动绑定测试脚本与用例 JSON，无需手动配置
- **基线快照** — 每次执行自动记录 OS、Java 版本、Godot 版本（MD5）等环境信息
- **Debug 日志收集** — 自动保存每个脚本的完整输出到本地日志目录，保留 7 天后自动清理

#### 手工测试
- **树形任务管理** — TaskGroup（任务组）→ ManualTestBatch（任务）→ ManualTestDetail（执行记录）三层结构
- **FromManager 保留任务组** — 从用例管理面板扫描到的用例统一归入 FromManager 任务，支持逐条手工执行记录
- **附件支持** — 手工测试结果可关联截图等附件文件

#### 缺陷管理
- 本地缺陷库，支持严重程度、优先级、状态跟踪

#### Git 集成
- **GitHub 面板** — 可视化管理 Git 远程仓库，一键 Push 用例文件到 GitHub

#### 测试报告
- 自动汇总执行结果，生成通过率统计报表
- 按严重程度和优先级维度展示分布图表

### 🔧 改进 & 优化

#### 性能优化
- **HashCache 哈希缓存** — 基于 dirty 标记的增量缓存系统，无文件变动时实现 O(1) 零扫描启动；切换项目目录时自动失效重建；异步持久化到磁盘
- **UI 布局持久化** — SplitPane 分割位置、窗口状态等自动记忆恢复

#### 设置系统
- **全局配置中心** — 统一的 GlobalConfig 管理用例格式、脚本语言、热键等配置
- **自定义热键** — 支持自定义 F5/F6/F7/F9/CTRL+N/CTRL+S 等快捷键
- **自定义语言扩展** — 可在设置面板添加任意语言的执行命令模板
- **缓存管理面板** — 可视化查看缓存状态，支持一键清除

#### 其他改进
- Windows 控制台 UTF-8 编码强制修复，解决中文乱码问题
- 目录历史记录（最近 10 个），快速切换项目
- 跨平台构建支持（Windows / Linux / macOS Maven Profile）

### 📦 技术栈

| 技术 | 版本 |
|------|------|
| Java | 17 |
| JavaFX | 17.0.14 |
| H2 Database | 2.3.232 |
| MyBatis | 3.5.16 |
| Jackson (JSON/XML/YAML) | 2.18.2 |
| JGit | 6.10.0 |
| JUnit 5 + TestFX | 5.10.2 / 4.0.18 |

---

## [1.0.0] - 初始版本

### 核心功能
- 用例 CRUD 管理（JSON 格式）
- 文件目录树浏览
- H2 本地数据库存储
- 基础 JavaFX 桌面界面
