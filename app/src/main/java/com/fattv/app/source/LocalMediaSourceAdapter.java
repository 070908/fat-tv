package com.fattv.app.source;

import android.net.Uri;

import com.fattv.app.model.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * LocalMediaSourceAdapter - 本地媒体文件音源适配器
 * 用于播放扫描到的本地音乐（内部存储 / 外置U盘）：
 * <ul>
 *   <li>search() 恒返回空列表——本地文件不参与关键词搜索</li>
 *   <li>resolveUrl() 将 Song.id（文件绝对路径或 content:// URI）解析为可播放 URI</li>
 * </ul>
 */
public class LocalMediaSourceAdapter implements SourceProvider {

    @Override
    public String getName() {
        return "\u672c\u5730\u97f3\u4e50";
    }

    @Override
    public SourceType getType() {
        return SourceType.LOCAL_MEDIA;
    }

    @Override
    public List<Song> search(String keyword, int page) throws Exception {
        return new ArrayList<>();
    }

    @Override
    public String resolveUrl(Song song) throws Exception {
        if (song == null || song.id == null) return null;
        // content:// URI（API 29+ MediaStore 可能不返回 DATA 列）直接可用
        if (song.id.startsWith("content://")) return song.id;
        // 文件绝对路径：转换为 file:// URI
        File f = new File(song.id);
        if (f.exists()) return Uri.fromFile(f).toString();
        return song.id;
    }

    @Override
    public String getLyric(Song song) throws Exception {
        return null;
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public void checkHealth() {
        // 本地文件始终可用
    }
}