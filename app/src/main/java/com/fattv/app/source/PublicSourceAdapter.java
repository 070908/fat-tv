package com.fattv.app.source;

import android.content.Context;

import com.fattv.app.jsengine.JSRuntime;
import com.fattv.app.model.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * 公网音源适配器 - 基于 JSRuntime (Rhino 引擎) 执行音源脚本
 */
public class PublicSourceAdapter implements SourceProvider {
    private static final String TAG = "PublicSource";
    private static final String DEFAULT_SCRIPT_PATH = "scripts/real-music.js";
    private static final String DEFAULT_SCRIPT_ID = "netease";
    private static final String DEFAULT_SCRIPT_NAME = "\u7f51\u6613\u4e91\u97f3\u4e50";

    private boolean healthy = false;
    private JSRuntime jsRuntime;
    private String pluginUrl; // 用户导入的自定义脚本路径/URL
    private Context appContext;

    public PublicSourceAdapter() {}

    public PublicSourceAdapter(String pluginUrl) {
        this.pluginUrl = pluginUrl;
    }

    /**
     * 初始化引擎并加载脚本。应在 SourceManager.init() 之后调用。
     */
    public void init(Context context) {
        this.appContext = context.getApplicationContext();
        jsRuntime = new JSRuntime();
        jsRuntime.init();

        boolean loaded;
        if (pluginUrl != null && !pluginUrl.isEmpty()) {
            // TODO: 从自定义路径/URL 加载脚本
            loaded = jsRuntime.loadScriptFromAssets(appContext, DEFAULT_SCRIPT_PATH, DEFAULT_SCRIPT_ID, DEFAULT_SCRIPT_NAME);
        } else {
            loaded = jsRuntime.loadScriptFromAssets(appContext, DEFAULT_SCRIPT_PATH, DEFAULT_SCRIPT_ID, DEFAULT_SCRIPT_NAME);
        }
        healthy = loaded;
    }

    @Override
    public String getName() {
        JSRuntime.ScriptInfo info = jsRuntime != null ? jsRuntime.getCurrentScriptInfo() : null;
        return info != null ? info.name : "\u516c\u7f51\u97f3\u6e90";
    }

    @Override
    public SourceType getType() {
        return SourceType.PUBLIC;
    }

    @Override
    public List<Song> search(String keyword, int page) throws Exception {
        if (jsRuntime == null) return new ArrayList<>();
        List<Song> result = jsRuntime.search(keyword, page);
        return result != null ? result : new ArrayList<>();
    }

    @Override
    public String resolveUrl(Song song) throws Exception {
        if (jsRuntime == null) return null;
        // 默认使用 128k 音质
        return jsRuntime.resolveUrl(song.id, "128k");
    }

    @Override
    public String getLyric(Song song) throws Exception {
        if (jsRuntime == null) return null;
        return jsRuntime.getLyric(song.id);
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public void checkHealth() {
        healthy = (jsRuntime != null && jsRuntime.getCurrentScriptInfo() != null);
    }

    public void setPluginUrl(String url) {
        this.pluginUrl = url;
        // TODO: 重新加载脚本
    }

    public void destroy() {
        if (jsRuntime != null) {
            jsRuntime.destroy();
            jsRuntime = null;
        }
        healthy = false;
    }
}
