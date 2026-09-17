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

        // 注册所有音源
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
}
