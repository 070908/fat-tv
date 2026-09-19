package com.fattv.app.source;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.fattv.app.model.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * LocalMusicScanner - 本地音乐扫描器
 * <p>三个来源：</p>
 * <ol>
 *   <li>MediaStore.Audio（内部存储 + 已索引的外部存储）</li>
 *   <li>/storage 下非 emulated 的挂载点（U盘等外部存储，递归扫描）</li>
 *   <li>内部存储常见音乐目录（/Music、/Download）兜底遍历</li>
 * </ol>
 * <p>扫描结果统一转换为 Song，sourceType = LOCAL_MEDIA，id 为文件绝对路径
 * 或 content:// URI，供 LocalMediaSourceAdapter.resolveUrl() 解析播放。</p>
 */
public class LocalMusicScanner {

    private static final String[] AUDIO_EXTS = {
            ".mp3", ".flac", ".wav", ".m4a", ".aac", ".ogg", ".ape", ".wma", ".opus"
    };
    private static final int MAX_DEPTH = 5;

    private LocalMusicScanner() {
    }

    /**
     * 全量扫描：MediaStore + 外置U盘 + 内部常见目录
     */
    public static List<Song> scanAll(Context context) {
        List<Song> result = new ArrayList<>();
        result.addAll(scanMediaStore(context));
        result.addAll(scanRemovableStorage());
        result.addAll(scanInternalMusicDirs(context));
        return result;
    }

    /**
     * 1. 通过 MediaStore 查询已索引的音频（内部存储 + 系统已索引的外部设备）
     */
    private static List<Song> scanMediaStore(Context context) {
        List<Song> list = new ArrayList<>();
        try {
            ContentResolver cr = context.getContentResolver();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA
            };
            Cursor c = cr.query(uri, projection, null, null,
                    MediaStore.Audio.Media.TITLE + " ASC");
            if (c != null) {
                while (c.moveToNext()) {
                    Song s = new Song();
                    String data = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    if (data != null && !data.isEmpty()) {
                        s.id = data;
                    } else {
                        long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                        s.id = ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString();
                    }
                    s.title = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    s.artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    s.album = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                    s.duration = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    if (s.title == null || s.title.isEmpty()) s.title = s.id;
                    if (s.artist == null || s.artist.isEmpty()) s.artist = "\u672a\u77e5\u827a\u672f\u5bb6";
                    s.sourceType = SourceProvider.SourceType.LOCAL_MEDIA;
                    s.sourceName = "\u672c\u5730\u97f3\u4e50";
                    list.add(s);
                }
                c.close();
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    /**
     * 2. 扫描 /storage 下的外置挂载点（U盘），排除 emulated/self
     */
    private static List<Song> scanRemovableStorage() {
        List<Song> list = new ArrayList<>();
        try {
            File storage = new File("/storage");
            File[] roots = storage.listFiles();
            if (roots == null) return list;
            for (File root : roots) {
                String name = root.getName();
                if (name.equals("emulated") || name.equals("self")) continue;
                scanDir(root, 0, list);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    /**
     * 3. 扫描内部存储常见音乐目录，作为 MediaStore 未索引文件的兜底
     */
    private static List<Song> scanInternalMusicDirs(Context context) {
        List<Song> list = new ArrayList<>();
        try {
            File external = context.getExternalFilesDir(null);
            if (external == null) return list;
            File parent = external.getParentFile(); // /storage/emulated/0/Android/data/...
            if (parent == null) return list;
            File root = parent.getParentFile(); // /storage/emulated/0
            if (root == null) root = external;
            scanDir(new File(root, "Music"), 0, list);
            scanDir(new File(root, "Download"), 0, list);
            scanDir(new File(root, "fatTV"), 0, list);
        } catch (Exception ignored) {
        }
        return list;
    }

    /**
     * 递归遍历目录收集音频文件（带隐藏目录过滤与深度上限）
     */
    private static void scanDir(File dir, int depth, List<Song> out) {
        if (dir == null || depth > MAX_DEPTH || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isHidden()) continue;
            if (f.isDirectory()) {
                scanDir(f, depth + 1, out);
            } else if (isAudioFile(f.getName())) {
                Song s = new Song();
                s.id = f.getAbsolutePath();
                String name = f.getName();
                int dot = name.lastIndexOf('.');
                s.title = dot > 0 ? name.substring(0, dot) : name;
                s.artist = "\u672a\u77e5\u827a\u672f\u5bb6";
                s.album = "\u672c\u5730\u6587\u4ef6";
                s.duration = 0;
                s.sourceType = SourceProvider.SourceType.LOCAL_MEDIA;
                s.sourceName = "\u672c\u5730\u97f3\u4e50";
                out.add(s);
            }
        }
    }

    private static boolean isAudioFile(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase(java.util.Locale.ROOT);
        for (String ext : AUDIO_EXTS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }
}