package com.fattv.app.source;

import android.content.Context;
import android.content.SharedPreferences;
import com.fattv.app.model.Song;
import java.util.ArrayList;
import java.util.List;

/**
 * SourceManager - 统一音源管理器
 * 负责：多源注册、健康检查、自动降级、手动切换
 */
public class SourceManager {
    private static final String PREFS_NAME = "fattv_settings";
    private static final String KEY_SOURCE_TYPE = "source_type";
    private static final String KEY_LOCAL_URL = "local_server_url";
    private static final String KEY_PUBLIC_PLUGIN = "public_plugin_url";
    private static final String KEY_PROXY_TYPE = "proxy_type";
    private static final String KEY_PROXY_HOST = "proxy_host";
    private static final String KEY_PROXY_PORT = "proxy_port";

    private static SourceManager instance;
    private final List<SourceProvider> providers = new ArrayList<>();
    private SourceProvider currentProvider;
    private Context context;

    private SourceManager() {}

    public static synchronized SourceManager getInstance() {
        if (instance == null) instance = new SourceManager();
        return instance;
    }

    public void init(Context ctx) {
        this.context = ctx.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String localUrl = prefs.getString(KEY_LOCAL_URL, "http://192.168.1.100:8090");
        String pluginUrl = prefs.getString(KEY_PUBLIC_PLUGIN, "");

        // 注册所有音源（避免重复注册：清空后再添加）
        providers.clear();
        PublicSourceAdapter publicAdapter = new PublicSourceAdapter(pluginUrl);
        publicAdapter.init(context);
        providers.add(publicAdapter);
        providers.add(new LocalSourceAdapter(localUrl));

        // 恢复上次选择的音源
        String savedType = prefs.getString(KEY_SOURCE_TYPE, "PUBLIC");
        setSourceByType(SourceProvider.SourceType.valueOf(savedType));
    }

    public void setSourceByType(SourceProvider.SourceType type) {
        for (SourceProvider p : providers) {
            if (p.getType() == type) {
                currentProvider = p;
                savePreference(KEY_SOURCE_TYPE, type.name());
                return;
            }
        }
    }

public SourceProvider getCurrentProvider() {
        return currentProvider;
    }

    /**
     * 按类型查找音源提供者（与 Song.sourceType 配套，用于跨源搜索结果解析播放地址）
     */
    public SourceProvider getProviderByType(SourceProvider.SourceType type) {
        for (SourceProvider p : providers) {
            if (p.getType() == type) return p;
        }
        return currentProvider;
    }

    /**
     * 多源聚合搜索：并发查询所有健康音源并合并结果，标注来源名称。
     * 供搜索页使用——第三方公网音源 + 局域网后端同时出结果。
     */
    public List<Song> searchAll(String keyword, int page) {
        List<Song> merged = new ArrayList<>();
        List<Thread> workers = new ArrayList<>();
        final Object lock = new Object();

        for (final SourceProvider p : providers) {
            if (!p.isHealthy()) continue;
            Thread t = new Thread(() -> {
                try {
                    List<Song> songs = p.search(keyword, page);
                    synchronized (lock) {
                        for (Song s : songs) {
                            if (s.sourceType == null) s.sourceType = p.getType();
                            if (s.sourceName == null || s.sourceName.isEmpty()) s.sourceName = p.getName();
                            merged.add(s);
                        }
                    }
                } catch (Exception ignored) {
                    // 单个源失败不阻塞其他源
                }
            });
            t.start();
            workers.add(t);
        }
        for (Thread t : workers) {
            try { t.join(); } catch (InterruptedException ignored) {}
        }
        return merged;
    }

    public List<SourceProvider> getAllProviders() {
        return providers;
    }

    public void checkAllHealth() {
        for (SourceProvider p : providers) {
            p.checkHealth();
        }
    }

    /**
     * 自动降级：当前源失效时尝试切换到其他可用源
     * @return 是否成功切换
     */
    public boolean autoFallback() {
        if (currentProvider != null && currentProvider.isHealthy()) return true;

        for (SourceProvider p : providers) {
            if (p != currentProvider && p.isHealthy()) {
                currentProvider = p;
                savePreference(KEY_SOURCE_TYPE, p.getType().name());
                return true;
            }
        }
        return false;
    }

    public boolean isCurrentHealthy() {
        return currentProvider != null && currentProvider.isHealthy();
    }

    public void setLocalServerUrl(String url) {
        savePreference(KEY_LOCAL_URL, url);
        for (SourceProvider p : providers) {
            if (p instanceof LocalSourceAdapter) {
                ((LocalSourceAdapter) p).setServerUrl(url);
                break;
            }
        }
    }

    public void setPublicPluginUrl(String url) {
        savePreference(KEY_PUBLIC_PLUGIN, url);
        for (SourceProvider p : providers) {
            if (p instanceof PublicSourceAdapter) {
                ((PublicSourceAdapter) p).setPluginUrl(url);
                break;
            }
        }
    }

    private void savePreference(String key, String value) {
        if (context != null) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(key, value).apply();
        }
    }

    public String getLocalServerUrl() {
        if (context == null) return "http://192.168.1.100:8090";
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOCAL_URL, "http://192.168.1.100:8090");
    }

    public String getPublicPluginUrl() {
        if (context == null) return "";
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PUBLIC_PLUGIN, "");
    }

    // ===== 代理配置 =====
    public void setProxyConfig(String type, String host, String port) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_PROXY_TYPE, type);
        editor.putString(KEY_PROXY_HOST, host);
        editor.putString(KEY_PROXY_PORT, port);
        editor.apply();
    }

    public String getProxyType() {
        if (context == null) return "HTTP";
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROXY_TYPE, "HTTP");
    }

    public String getProxyHost() {
        if (context == null) return "";
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROXY_HOST, "");
    }

    public String getProxyPort() {
        if (context == null) return "";
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROXY_PORT, "");
    }
}
