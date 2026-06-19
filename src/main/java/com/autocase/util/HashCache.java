package com.autocase.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 哈希缓存管理器 - 基于脏标记的增量缓存
 *
 * <h3>核心设计：脏标记（dirty flag）模式</h3>
 * <ul>
 *   <li><b>rootHash</b>: 仅基于目录绝对路径的 MD5。只有切换根目录时才会变化</li>
 *   <li><b>dirty 标记</b>: 布尔值，记录目录自上次扫描以来是否有文件变动</li>
 * </ul>
 *
 * <h3>缓存策略（零遍历设计）</h3>
 * <table>
 *   <tr><th>场景</th><th>行为</th><th>开销</th></tr>
 *   <tr><td>首次访问 / 无缓存</td><td>全量扫描 → 写入缓存 + 清 dirty</td><td>O(n) 扫描</td></tr>
 *   <tr><td>rootHash 未变 + dirty=false</td><td>直接返回内存缓存（零遍历）</td><td>O(1)</td></tr>
 *   <tr><td>rootHash 未变 + dirty=true</td><td>需要重扫，扫完后自动清 dirty</td><td>O(n) 扫描</td></tr>
 *   <tr><td>rootHash 变了（换项目）</td><td>旧缓存失效，全量重扫</td><td>O(n) 扫描</td></tr>
 *   <tr><td>外部通知文件变动</td><td>notifyFileChange() → 置 dirty=true</td><td>O(1)</td></tr>
 * </table>
 *
 * <h3>关键优势</h3>
 * <p>正常使用时（无文件变动），getOrCheck() 不会触发任何文件系统遍历，
 * 直接从内存 Map 返回缓存数据，实现真正的 O(1) 命中。</p>
 *
 * <p>典型使用流程：</p>
 * <pre>
 *   CacheResult&lt;List&lt;TestCase&gt;&gt; result = cache.getOrCheck(directoryPath, CacheType.CASES);
 *
 *   if (result.isFresh()) {
 *       // 缓存新鲜，直接用（零扫描）
 *       allCases = result.getData();
 *   } else {
 *       // 缓存过期（dirty=true 或换项目了），执行全量扫描
 *       allCases = caseDao.scanCases(directoryPath);
 *       cache.put(directoryPath, CacheType.CASES, new ArrayList&lt;&gt;(allCases));
 *       // put() 内部会自动清除 dirty 标记
 *   }
 * </pre>
 */
public class HashCache {

    /** 缓存根目录 */
    private static final String CACHE_DIR_NAME = ".cms_cache";

    /** 缓存版本号（结构变更时递增，自动使旧缓存失效） */
    private static final int CACHE_VERSION = 3;

    /** 缓存条目类型枚举 */
    public enum CacheType {
        CASES("cases"),          // 用例列表
        SCRIPTS("scripts"),      // 脚本列表
        FILE_TREE("file_tree");   // 文件树结构

        final String key;
        CacheType(String key) { this.key = key; }
    }

    /** 内存中的缓存实例（单例） */
    private static HashCache instance;

    /** 缓存存储：compositeKey -> CacheEntry */
    private final Map<String, CacheEntry> cacheStore = new ConcurrentHashMap<>();

    /** rootHash 记录：directoryPath -> MD5(目录路径) */
    private final Map<String, String> rootHashRecords = new ConcurrentHashMap<>();

    /**
     * 脏标记记录：compositeKey -> boolean
     * true = 自上次 put() 以来有文件变动，需要重扫
     * false = 无变动，缓存可直接使用
     */
    private final Map<String, Boolean> dirtyFlags = new ConcurrentHashMap<>();

    /** 缓存根目录 File 对象 */
    private final File cacheRootDir;

    // ==================== 单例 ====================

    private HashCache() {
        cacheRootDir = new File(System.getProperty("user.home"), CACHE_DIR_NAME);
        if (!cacheRootDir.exists()) {
            cacheRootDir.mkdirs();
        }
    }

    public static synchronized HashCache getInstance() {
        if (instance == null) {
            instance = new HashCache();
        }
        return instance;
    }

    // ==================== 公开 API：哈希计算 ====================

    /**
     * 计算根目录哈希（仅基于目录路径字符串）
     * 只要用户没切换项目目录，这个值就不会变
     */
    public String computeRootHash(String directoryPath) {
        if (directoryPath == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(directoryPath.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 公开 API：缓存读写 ====================

    /**
     * 检查缓存状态并返回结果（基于脏标记，零遍历设计）
     *
     * <p>判定逻辑（不遍历文件系统）：</p>
     * <ol>
     *   <li>rootHash 变了 → rootChanged=true，需要全量重扫（换项目了）</li>
     *   <li>rootHash 未变 + dirty=false → fresh=true，直接返回缓存（零开销）</li>
     *   <li>rootHash 未变 + dirty=true → fresh=false，需要重扫（文件有变动）</li>
     * </ol>
     *
     * @param directoryPath 目录路径
     * @param type          缓存类型
     * @return CacheResult 包含新鲜度状态和数据（可能为null），调用方需自行强转 data 到目标类型
     */
    public CacheResult<Object> getOrCheck(String directoryPath, CacheType type) {
        String compositeKey = type.key + "@" + directoryPath;
        String currentRootHash = computeRootHash(directoryPath);

        // 根目录不存在
        if (currentRootHash == null) {
            return new CacheResult<>(false, true, null);
        }

        String storedRootHash = rootHashRecords.get(compositeKey);

        // 首次访问该目录 或 根目录已切换 → 完全无效
        if (storedRootHash == null || !storedRootHash.equals(currentRootHash)) {
            Object diskData = loadFromDisk(type);
            return new CacheResult<>(false, true, diskData);
        }

        // rootHash 一致，检查脏标记（O(1)，不遍历文件系统）
        Boolean isDirty = dirtyFlags.get(compositeKey);
        if (isDirty == null) {
            isDirty = true;
        }

        if (isDirty) {
            Object oldData = getFromMemory(compositeKey);
            if (oldData == null) {
                oldData = loadFromDisk(type);
            }
            return new CacheResult<>(false, false, oldData);
        }

        // 完全命中（rootHash 一致 + 干净）→ fresh
        Object data = getFromMemory(compositeKey);
        if (data != null) {
            return new CacheResult<>(true, false, data);
        }

        // 内存没有但磁盘有（比如重启后第一次访问，且无变动）
        data = loadFromDisk(type);
        if (data != null) {
            cacheStore.put(compositeKey, new CacheEntry(data, System.currentTimeMillis(), currentRootHash));
            return new CacheResult<>(true, false, data);
        }

        return new CacheResult<>(false, false, null);
    }

    /**
     * 将数据写入缓存（同时更新 rootHash 并清除脏标记）
     *
     * <p>调用此方法意味着调用方已经完成了数据刷新（全量扫描），
     * 因此自动将 dirty 标记置为 false。</p>
     *
     * @param directoryPath 目录路径
     * @param type          缓存类型
     * @param data          要缓存的数据
     * @param <T>            数据类型
     */
    public <T extends Serializable> void put(String directoryPath, CacheType type, T data) {
        String compositeKey = type.key + "@" + directoryPath;
        String rootHash = computeRootHash(directoryPath);
        if (rootHash == null) return;

        CacheEntry entry = new CacheEntry(data, System.currentTimeMillis(), rootHash);
        cacheStore.put(compositeKey, entry);
        rootHashRecords.put(compositeKey, rootHash);
        // 写入缓存后清除脏标记（数据已是最新）
        dirtyFlags.put(compositeKey, false);

        asyncPersist(type, entry, compositeKey);
    }

    /**
     * 外部通知：某个文件发生了变动（创建/删除/修改内容）
     * 调用后，下次 getOrCheck 会返回 stale 状态，提示调用方刷新数据
     *
     * <p>此操作是 O(1) 的，不会触发任何文件系统遍历。</p>
     *
     * @param directoryPath 所属目录
     * @param filePath      变动的文件路径（可为 null，null 表示未知哪个文件变了）
     */
    public void notifyFileChange(String directoryPath, String filePath) {
        if (directoryPath == null) return;
        for (CacheType type : CacheType.values()) {
            String compositeKey = type.key + "@" + directoryPath;
            // 仅置脏标记，不清除缓存数据（调用方决定何时重扫）
            dirtyFlags.put(compositeKey, true);
        }
    }

    /**
     * 使指定类型的缓存失效
     */
    public void invalidate(CacheType type) {
        cacheStore.entrySet().removeIf(e -> e.getKey().startsWith(type.key + "@"));
        rootHashRecords.keySet().removeIf(k -> k.startsWith(type.key + "@"));
        dirtyFlags.keySet().removeIf(k -> k.startsWith(type.key + "@"));
        deleteCacheFile(type);
    }

    /**
     * 使指定目录相关的所有缓存失效
     */
    public void invalidateDirectory(String directoryPath) {
        if (directoryPath == null) return;
        for (CacheType type : CacheType.values()) {
            String compositeKey = type.key + "@" + directoryPath;
            cacheStore.remove(compositeKey);
            rootHashRecords.remove(compositeKey);
            dirtyFlags.remove(compositeKey);
        }
        for (CacheType type : CacheType.values()) {
            deleteCacheFile(type);
        }
    }

    /**
     * 清除全部缓存
     */
    public void clearAll() {
        cacheStore.clear();
        rootHashRecords.clear();
        dirtyFlags.clear();
        for (CacheType type : CacheType.values()) {
            deleteCacheFile(type);
        }
    }

    /**
     * 获取缓存统计信息（用于调试/设置面板展示）
     */
    public String getStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== HashCache 统计 (v").append(CACHE_VERSION).append(" 脏标记模式) ===\n");
        for (CacheType type : CacheType.values()) {
            int memCount = 0;
            Object lastData = null;
            long lastTime = 0;
            int dirtyCount = 0;
            int cleanCount = 0;
            for (Map.Entry<String, CacheEntry> e : cacheStore.entrySet()) {
                if (e.getKey().startsWith(type.key + "@")) {
                    memCount++;
                    lastData = e.getValue().data;
                    lastTime = e.getValue().timestamp;
                    Boolean dirty = dirtyFlags.get(e.getKey());
                    if (Boolean.TRUE.equals(dirty)) dirtyCount++;
                    else cleanCount++;
                }
            }
            if (memCount > 0 && lastData != null) {
                sb.append(String.format("  %-12s | %d 个目录缓存 | 脏:%d 净:%d | 最新: %s | %s\n",
                        type.key, memCount, dirtyCount, cleanCount,
                        formatSize(lastData), formatTime(lastTime)));
            } else {
                boolean existsOnDisk = getCacheFile(type).exists();
                sb.append(String.format("  %-12s | 内存无 | 磁盘: %s\n",
                        type.key, existsOnDisk ? "有" : "无"));
            }
        }
        sb.append("  缓存目录: ").append(cacheRootDir.getAbsolutePath()).append("\n");
        sb.append("  已跟踪目录数: ").append(rootHashRecords.size()).append("\n");
        return sb.toString();
    }

    // ==================== 内部方法 ====================

    /**
     * 从内存中获取缓存数据（按 compositeKey 精确查找）
     */
    private Serializable getFromMemory(String compositeKey) {
        CacheEntry entry = cacheStore.get(compositeKey);
        if (entry != null) {
            return (Serializable) entry.data;
        }
        return null;
    }

    /**
     * 异步持久化到磁盘
     */
    private void asyncPersist(CacheType type, CacheEntry entry, String compositeKey) {
        new Thread(() -> {
            try {
                File file = getCacheFile(type);
                try (ObjectOutputStream oos = new ObjectOutputStream(
                        new BufferedOutputStream(new FileOutputStream(file)))) {
                    oos.writeInt(CACHE_VERSION);
                    oos.writeUTF(entry.rootHash);
                    oos.writeLong(entry.timestamp);
                    oos.writeObject(entry.data);
                }
            } catch (IOException e) {
                // 静默失败
            }
        }, "HashCache-Persist-" + type.key).start();
    }

    /**
     * 从磁盘加载缓存（返回原始 Serializable，由调用方负责类型转换）
     */
    private Serializable loadFromDisk(CacheType type) {
        File file = getCacheFile(type);
        if (!file.exists()) return null;

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            int version = ois.readInt();
            if (version != CACHE_VERSION) {
                file.delete();
                return null;
            }
            // v3 格式：版本号 + rootHash + 时间戳 + 数据（不再存储 fileHash）
            String rootHash = ois.readUTF();
            long timestamp = ois.readLong();
            Object data = ois.readObject();

            return (Serializable) data;
        } catch (IOException | ClassNotFoundException e) {
            file.delete();
            return null;
        }
    }

    private File getCacheFile(CacheType type) {
        return new File(cacheRootDir, "cache_" + type.key + ".dat");
    }

    private void deleteCacheFile(CacheType type) {
        File file = getCacheFile(type);
        if (file.exists()) {
            file.delete();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String formatSize(Object data) {
        if (data instanceof Collection) {
            return ((Collection<?>) data).size() + " 条";
        }
        if (data == null) return "null";
        return "?";
    }

    private static String formatTime(long ts) {
        return new Date(ts).toString();
    }

    // ==================== 公开内部数据结构 ====================

    /**
     * 缓存查询结果
     *
     * @param <T> 数据类型
     */
    public static class CacheResult<T> {
        /** 缓存是否新鲜（true = 可直接使用，false = 需要刷新） */
        private final boolean fresh;
        /** 根目录是否发生了变化（true = 完全换了项目） */
        private final boolean rootChanged;
        /** 缓存的数据（fresh=true 时一定非null；stale 时可能是旧数据或null） */
        private final T data;

        CacheResult(boolean fresh, boolean rootChanged, T data) {
            this.fresh = fresh;
            this.rootChanged = rootChanged;
            this.data = data;
        }

        public boolean isFresh() { return fresh; }
        public boolean isRootChanged() { return rootChanged; }
        public T getData() { return data; }
    }

    /**
     * 缓存条目（v3：不再存储 fileHash，由 dirty 标记替代）
     */
    private static class CacheEntry implements Serializable {
        private static final long serialVersionUID = 3L;
        final Object data;
        final long timestamp;
        final String rootHash;

        CacheEntry(Object data, long timestamp, String rootHash) {
            this.data = data;
            this.timestamp = timestamp;
            this.rootHash = rootHash;
        }
    }
}
