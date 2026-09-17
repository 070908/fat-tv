package com.fattv.app.source;

import com.fattv.app.model.Song;
import com.fattv.app.utils.HttpUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * 局域网音源后端适配器 - 调用 xiaomusic / LX Server HTTP API
 */
public class LocalSourceAdapter implements SourceProvider {
    private static final String TAG = "LocalSource";
    private String serverUrl = "http://192.168.1.100:8090";
    private boolean healthy = true;

    public LocalSourceAdapter() {}

    public LocalSourceAdapter(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @Override
    public String getName() {
        return "\u5c40\u57df\u7f51\u97f3\u6e90";
    }

    @Override
    public SourceType getType() {
        return SourceType.LOCAL;
    }

    @Override
    public List<Song> search(String keyword, int page) throws Exception {
        String url = serverUrl + "/search?q=" + java.net.URLEncoder.encode(keyword, "UTF-8");
        String response = HttpUtils.get(url);
        List<Song> result = new ArrayList<>();
        JSONArray arr = new JSONArray(response);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Song s = new Song();
            s.id = o.optString("id", "local_" + i);
            s.title = o.optString("title", "");
            s.artist = o.optString("artist", "");
            s.album = o.optString("album", "");
            s.duration = o.optLong("duration", 0);
            s.sourceType = SourceType.LOCAL;
            result.add(s);
        }
        return result;
    }

    @Override
    public String resolveUrl(Song song) throws Exception {
        return serverUrl + "/play?id=" + song.id;
    }

    @Override
    public String getLyric(Song song) throws Exception {
        try {
            String url = serverUrl + "/lyric?id=" + song.id;
            return HttpUtils.get(url);
        } catch (Exception e) {
            return "[00:00.00]\u6682\u65e0\u6b4c\u8bcd";
        }
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public void checkHealth() {
        try {
            HttpUtils.get(serverUrl + "/health");
            healthy = true;
        } catch (Exception e) {
            healthy = false;
        }
    }

    public void setServerUrl(String url) {
        this.serverUrl = url;
    }
}
