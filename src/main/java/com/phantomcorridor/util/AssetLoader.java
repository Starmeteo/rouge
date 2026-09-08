package com.phantomcorridor.util;

/**
 * 资源加载工具（§9.1 util.AssetLoader；无状态通用工具）。
 *
 * <p>统一封装类路径资源（样式表、图片、音频）的查找，避免各 view 分别拼 URL 出错。
 * 预留了按主题命名空间查找的入口，后续可平滑替换为图片资源（§5.4 预留 Assets 接口）。
 */
public final class AssetLoader {

    /** 工具类：不允许实例化 */
    private AssetLoader() {
    }

    /**
     * 查找类路径资源并返回其外链 URL 字符串（相对当前模块/包根）。
     *
     * @param ownerClass 用于定位资源的类（以其所在包为根）
     * @param relative   相对该类的资源路径，如 {@code "ui/ui.css"}
     * @return 资源 URL 字符串；未找到返回 {@code null}
     */
    public static String url(Class<?> ownerClass, String relative) {
        java.net.URL resource = ownerClass.getResource(relative);
        return resource == null ? null : resource.toExternalForm();
    }

    /**
     * 直接按类路径查找并返回 URL 字符串（以 {@code /} 开头表示从类路径根查找）。
     *
     * @param classpathPath 类路径绝对路径，如 {@code "/com/phantomcorridor/ui/ui.css"}
     * @return 资源 URL 字符串；未找到返回 {@code null}
     */
    public static String urlFromClasspath(String classpathPath) {
        java.net.URL resource = AssetLoader.class.getResource(classpathPath);
        return resource == null ? null : resource.toExternalForm();
    }
}
