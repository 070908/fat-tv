package com.fattv.app.source;

import com.fattv.app.model.Song;
import java.util.List;

public interface SourceProvider {
    enum SourceType {
        PUBLIC,    // 公网音源直连（默认）
        LOCAL,     // 局域网音源后端
        LOCAL_MEDIA // 本地媒体文件
    }

    String getName();
    SourceType getType();

    List<Song> search(String keyword, int page) throws Exception;
    String resolveUrl(Song song) throws Exception;
    String getLyric(Song song) throws Exception;

    boolean isHealthy();
    void checkHealth();
}
