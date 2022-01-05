package edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;

import java.util.Collection;
import java.util.HashMap;

/**
 * @Data        :2020/06/22
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :全局单例模式 gtb 根节点缓存器
 */

public enum GTBRootCache {
    /* 单例模式 GTB 文件根节点 */
    INSTANCE;

    private final HashMap<String, GTBManager> cache = new HashMap<>(4);

    /**
     * 获取管理器
     */
    public static GTBManager get(String fileName) {
        Assert.NotEmpty(fileName);

        synchronized (INSTANCE.cache) {
            if (!INSTANCE.cache.containsKey(fileName)) {
                INSTANCE.cache.put(fileName, new GTBManager(fileName));
            }

            return INSTANCE.cache.get(fileName);
        }
    }

    /**
     * 获取管理器
     */
    public static GTBManager[] get(String... fileNames) {
        GTBManager[] managers = new GTBManager[fileNames.length];

        for (int i = 0; i < fileNames.length; i++) {
            managers[i] = get(fileNames[i]);
        }
        return managers;
    }

    /**
     * 获取管理器
     */
    public static GTBManager[] get(Collection<String> fileNames) {
        return get(ArrayUtils.toStringArray(fileNames));
    }

    /**
     * 获取当前管理器储存的数量
     */
    public static int size() {
        synchronized (INSTANCE.cache) {
            return INSTANCE.cache.size();
        }
    }

    /**
     * 清除指定的管理器数据
     */
    public static void clear() {
        synchronized (INSTANCE.cache) {
            INSTANCE.cache.clear();
        }
    }

    /**
     * 清除指定的管理器数据
     */
    public static void clear(String... fileNames) {
        synchronized (INSTANCE.cache) {
            for (String fileName : fileNames) {
                INSTANCE.cache.remove(fileName);
            }
        }
    }

    /**
     * 清除指定的管理器数据
     */
    public static void clear(GTBManager... managers) {
        synchronized (INSTANCE.cache) {
            for (GTBManager manager : managers) {
                INSTANCE.cache.remove(manager.getFileName());
            }
        }
    }

    /**
     * 清除指定的管理器数据
     */
    public static <ManagerType> void clear(Collection<ManagerType> managers) {
        synchronized (INSTANCE.cache) {
            for (ManagerType managerUnknownType : managers) {
                if (managerUnknownType instanceof String) {
                    INSTANCE.cache.remove((String) managerUnknownType);
                } else if (managerUnknownType instanceof GTBManager) {
                    INSTANCE.cache.remove(((GTBManager) managerUnknownType).getFileName());
                }
            }
        }
    }

    /**
     * 检验缓存器中是否存在该管理器
     */
    public static boolean contain(String fileName) {
        synchronized (INSTANCE.cache) {
            return INSTANCE.cache.containsKey(fileName);
        }
    }

    /**
     * 获取管理器的名字
     */
    public static String[] getNames(GTBManager... managers) {
        String[] names = new String[managers.length];
        for (int i = 0; i < managers.length; i++) {
            names[i] = managers[i].getFileName();
        }

        return names;
    }
}
