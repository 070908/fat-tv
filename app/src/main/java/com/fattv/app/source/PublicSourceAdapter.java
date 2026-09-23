package com.fattv.app.source;

import android.content.Context;

import com.fattv.app.jsengine.JSRuntime;
import com.fattv.app.model.Song;

import org.json.JSONArray;
import org.json.JSONObject;

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

    /**
     * 获取热门排行榜歌曲（JS 脚本 getTopSongs）
     */
    public List<Song> getTopSongs(int limit) {
        if (jsRuntime == null) return new ArrayList<>();
        JSONArray arr = jsRuntime.callArrayMethod("getTopSongs", limit);
        return parseSongArray(arr);
    }

    /**
     * 获取热门专辑列表（JS 脚本 getTopAlbums）
     */
    public List<Song> getTopAlbums(int limit) {
        if (jsRuntime == null) return new ArrayList<>();
        JSONArray arr = jsRuntime.callArrayMethod("getTopAlbums", limit);
        List<Song> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject o = arr.getJSONObject(i);
                Song s = new Song();
                s.id = String.valueOf(o.optLong("id", 0));
                s.title = o.optString("name", "");
                s.album = o.optString("name", "");
                s.artist = o.optString("artist", "");
                s.coverUrl = o.optString("pic", "");
                s.sourceType = SourceProvider.SourceType.PUBLIC;
                list.add(s);
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    /**
     * 获取热门 MV 列表（JS 脚本 getTopMvs）
     */
    public List<Song> getTopMvs(int limit) {
        if (jsRuntime == null) return new ArrayList<>();
        JSONArray arr = jsRuntime.callArrayMethod("getTopMvs", limit);
        List<Song> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject o = arr.getJSONObject(i);
                Song s = new Song();
                s.id = String.valueOf(o.optLong("id", 0));
                s.title = o.optString("name", "");
                s.artist = o.optString("artist", "");
                s.coverUrl = o.optString("pic", o.optString("cover", ""));
                s.duration = o.optLong("duration", 0);
                s.sourceType = SourceProvider.SourceType.PUBLIC;
                list.add(s);
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    /**
     * 解析 MV 播放地址（JS 脚本 resolveMvUrl）
     */
    public String resolveMvUrl(String mvId) {
        if (jsRuntime == null) return null;
        try {
            JSONObject o = jsRuntime.callObjectMethod("resolveMvUrl", mvId);
            return o != null ? o.optString("url", null) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取精选歌单列表（JS 脚本 getTopPlaylists）
     */
    public List<Song> getTopPlaylists(int limit) {
        if (jsRuntime == null) return new ArrayList<>();
        JSONArray arr = jsRuntime.callArrayMethod("getTopPlaylists", limit);
        List<Song> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject o = arr.getJSONObject(i);
                Song s = new Song();
                s.id = String.valueOf(o.optLong("id", 0));
                s.title = o.optString("name", "");
                s.album = o.optString("name", "");
                s.artist = o.optString("artist", "");
                s.coverUrl = o.optString("pic", "");
                s.sourceType = SourceProvider.SourceType.PUBLIC;
                list.add(s);
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    /**
     * 获取歌单内歌曲列表（JS 脚本 getPlaylistTracks）
     */
    public List<Song> getPlaylistTracks(String playlistId) {
        if (jsRuntime == null) return new ArrayList<>();
        JSONArray arr = jsRuntime.callArrayMethod("getPlaylistTracks", playlistId);
        return parseSongArray(arr);
    }

    /**
     * 获取专辑内歌曲列表（JS 脚本 getAlbumTracks，albumName 用于详情接口被风控时降级搜索）
     */
    public List<Song> getAlbumTracks(String albumId, String albumName) {
        if (jsRuntime == null) return new ArrayList<>();
        JSONArray arr = jsRuntime.callArrayMethod("getAlbumTracks", albumId, albumName);
        return parseSongArray(arr);
    }

    /**
     * 获取专辑内歌曲列表（JS 脚本 getAlbumTracks，单参兼容）
     */
    public List<Song> getAlbumTracks(String albumId) {
        return getAlbumTracks(albumId, null);
    }

    private List<Song> parseSongArray(JSONArray arr) {
        List<Song> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject o = arr.getJSONObject(i);
                Song s = new Song();
                s.id = String.valueOf(o.optLong("id", 0));
                s.title = o.optString("name", o.optString("title", ""));
                s.artist = o.optString("artist", "");
                s.album = o.optString("album", "");
                s.albumId = o.optString("albumId", "");
                s.coverUrl = o.optString("pic", o.optString("cover", ""));
                s.duration = o.optLong("duration", 0);
                s.sourceType = SourceProvider.SourceType.PUBLIC;
                list.add(s);
            } catch (Exception ignored) {
            }
        }
        return list;
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
