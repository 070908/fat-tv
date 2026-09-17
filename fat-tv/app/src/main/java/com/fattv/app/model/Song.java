package com.fattv.app.model;

import com.fattv.app.source.SourceProvider;
import java.io.Serializable;

/**
 * 歌曲数据模型
 */
public class Song implements Serializable {
    public String id;
    public String title;
    public String artist;
    public String album;
    public String coverUrl;
    public long duration; // ms
    public SourceProvider.SourceType sourceType;

    @Override
    public String toString() {
        return title + " - " + artist;
    }
}
