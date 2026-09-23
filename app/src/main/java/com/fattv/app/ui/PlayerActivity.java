package com.fattv.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.fattv.app.R;
import com.fattv.app.model.Song;
import com.fattv.app.player.MusicPlaybackService;
import com.fattv.app.source.PublicSourceAdapter;
import com.fattv.app.source.SourceManager;
import com.fattv.app.source.SourceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * PlayerActivity - TV 播放器页：金色主题、多行歌词面板（当前行高亮居中滚动）、
 * 歌词/专辑 Tab 切换（DPad 左右键）、专辑内歌曲列表点击切歌。
 */
public class PlayerActivity extends AppCompatActivity {
    private static final int MODE_LYRIC = 0;
    private static final int MODE_ALBUM = 1;

    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private TextView tabLyric, tabAlbum, tvAlbumTitle;
    private ImageView ivCover, ivPlayPause, ivNext, ivPrev, bgBlur;
    private ProgressBar progressBar;
    private RecyclerView rvLyric, rvAlbumSongs;
    private LinearLayout albumContainer;

    private Song currentSong;
    private boolean isPlaying = false;
    private Handler progressHandler;
    private Runnable progressRunnable;
    private int viewMode = MODE_LYRIC;

    private final List<Song> queue = new ArrayList<>();
    private int currentIndex = 0;

    // 歌词
    private final List<LyricLine> lyricLines = new ArrayList<>();
    private LyricAdapter lyricAdapter;
    private int activeLyricIndex = -1;

    // 专辑歌曲
    private final List<Song> albumSongs = new ArrayList<>();
    private AlbumSongAdapter albumAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // 绑定视图
        tvTitle = findViewById(R.id.tv_title);
        tvArtist = findViewById(R.id.tv_artist);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        tabLyric = findViewById(R.id.tab_lyric);
        tabAlbum = findViewById(R.id.tab_album);
        tvAlbumTitle = findViewById(R.id.tv_album_title);
        ivCover = findViewById(R.id.iv_cover);
        ivPlayPause = findViewById(R.id.btn_play_pause);
        ivNext = findViewById(R.id.btn_next);
        ivPrev = findViewById(R.id.btn_prev);
        bgBlur = findViewById(R.id.bg_blur);
        progressBar = findViewById(R.id.progress_bar);
        rvLyric = findViewById(R.id.rv_lyric);
        rvAlbumSongs = findViewById(R.id.rv_album_songs);
        albumContainer = findViewById(R.id.album_container);

        // 歌词列表
        rvLyric.setLayoutManager(new LinearLayoutManager(this));
        lyricAdapter = new LyricAdapter(lyricLines);
        rvLyric.setAdapter(lyricAdapter);

        // 专辑歌曲列表
        rvAlbumSongs.setLayoutManager(new LinearLayoutManager(this));
        albumAdapter = new AlbumSongAdapter(albumSongs);
        rvAlbumSongs.setAdapter(albumAdapter);

        // 焦点设置
        setupFocus(ivPlayPause);
        setupFocus(ivNext);
        setupFocus(ivPrev);

        // Tab 点击 + 焦点
        tabLyric.setOnClickListener(v -> switchMode(MODE_LYRIC));
        tabAlbum.setOnClickListener(v -> switchMode(MODE_ALBUM));
        setupTabFocus(tabLyric);
        setupTabFocus(tabAlbum);

        // 初始化模式
        switchMode(MODE_LYRIC);
        tabLyric.requestFocus();

        // 初始化进度条更新
        progressHandler = new Handler();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                updateProgress();
                updateLyricHighlight();
                progressHandler.postDelayed(this, 300);
            }
        };

        // 加载歌曲与队列
        currentSong = (Song) getIntent().getSerializableExtra("song");
        @SuppressWarnings("unchecked")
        ArrayList<Song> extraQueue = (ArrayList<Song>) getIntent().getSerializableExtra("queue");
        if (extraQueue != null && !extraQueue.isEmpty()) {
            queue.clear();
            queue.addAll(extraQueue);
        } else if (currentSong != null) {
            queue.add(currentSong);
        }

        if (currentSong != null) {
            currentIndex = findIndexById(currentSong);
            bindSongInfo(currentSong);
            playCurrentSong();
        }

        // 点击事件
        ivPlayPause.setOnClickListener(v -> togglePlay());
        ivNext.setOnClickListener(v -> skipNext());
        ivPrev.setOnClickListener(v -> skipPrev());
    }

    private int findIndexById(Song song) {
        for (int i = 0; i < queue.size(); i++) {
            Song s = queue.get(i);
            if (s != null && s.id != null && s.id.equals(song.id)) return i;
        }
        return 0;
    }

    private void switchTo(int index) {
        if (queue.isEmpty()) return;
        currentIndex = (index + queue.size()) % queue.size();
        currentSong = queue.get(currentIndex);
        lyricLines.clear();
        lyricAdapter.notifyDataSetChanged();
        activeLyricIndex = -1;
        bindSongInfo(currentSong);
        playCurrentSong();
    }

    private void bindSongInfo(Song song) {
        tvTitle.setText(song.title);
        tvArtist.setText(song.artist + (song.album != null && !song.album.isEmpty() ? "  •  " + song.album : ""));
        tvTotalTime.setText(formatDuration(song.duration));

        // 加载封面图到主封面和背景
        if (song.coverUrl != null && !song.coverUrl.isEmpty()) {
            Glide.with(this)
                .load(song.coverUrl)
                .placeholder(R.drawable.placeholder_cover)
                .error(R.drawable.placeholder_cover)
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .centerCrop()
                .into(ivCover);

            Glide.with(this)
                .load(song.coverUrl)
                .placeholder(R.drawable.placeholder_cover)
                .error(R.drawable.placeholder_cover)
                .centerCrop()
                .into(bgBlur);
        }

        loadLyric(song);
        loadAlbumSongs(song);
    }

    private void playCurrentSong() {
        Intent serviceIntent = new Intent(this, MusicPlaybackService.class);
        startService(serviceIntent);
        new Handler().postDelayed(() -> {
            MusicPlaybackService service = getServiceInstance();
            if (service != null) {
                service.playSong(currentSong);
                isPlaying = true;
                ivPlayPause.setImageResource(R.drawable.ic_pause);
                progressHandler.removeCallbacks(progressRunnable);
                progressHandler.post(progressRunnable);
            }
        }, 500);
    }

    /**
     * 异步加载歌词：优先来源 provider 提供，失败则显示占位
     */
    private void loadLyric(Song song) {
        if (song == null || song.sourceType == null) return;
        updateLyricUi(lyricLines.size(), "加载歌词中...");
        new Thread(() -> {
            try {
                SourceProvider provider = SourceManager.getInstance().getProviderByType(song.sourceType);
                String lrc = provider.getLyric(song);
                runOnUiThread(() -> parseLyric(lrc));
            } catch (Exception e) {
                runOnUiThread(() -> parseLyric(null));
            }
        }).start();
    }

    private void parseLyric(String lrc) {
        lyricLines.clear();
        if (lrc == null || lrc.trim().isEmpty()) {
            lyricAdapter.notifyDataSetChanged();
            return;
        }
        for (String line : lrc.split("\\n")) {
            if (line == null || line.trim().isEmpty()) continue;
            LyricLine ll = LyricLine.parse(line);
            if (ll != null) lyricLines.add(ll);
        }
        if (lyricLines.isEmpty()) {
            // 无时间戳的纯文本歌词，作为单行显示
            LyricLine plain = new LyricLine();
            plain.time = 0;
            plain.text = lrc.trim();
            lyricLines.add(plain);
        }
        lyricAdapter.notifyDataSetChanged();
        if (viewMode == MODE_LYRIC && !lyricLines.isEmpty()) {
            rvLyric.smoothScrollToPosition(0);
        }
    }

    private void updateLyricUi(int index, String text) {
        lyricLines.clear();
        if (text != null) {
            LyricLine plain = new LyricLine();
            plain.time = 0;
            plain.text = text;
            lyricLines.add(plain);
        }
        lyricAdapter.notifyDataSetChanged();
    }

    private void updateLyricHighlight() {
        if (lyricLines.isEmpty()) return;
        MusicPlaybackService service = getServiceInstance();
        long current = service != null ? service.getCurrentPosition() : 0;
        int index = 0;
        for (int i = lyricLines.size() - 1; i >= 0; i--) {
            if (current >= lyricLines.get(i).time) {
                index = i;
                break;
            }
        }
        if (index != activeLyricIndex) {
            int old = activeLyricIndex;
            activeLyricIndex = index;
            lyricAdapter.notifyItemChanged(old);
            lyricAdapter.notifyItemChanged(index);
            if (viewMode == MODE_LYRIC) {
                rvLyric.smoothScrollToPosition(index);
            }
        }
    }

    /**
     * 加载专辑歌曲：优先当前队列同专辑歌曲；若 Song 带 albumId 再异步拉取完整专辑列表
     */
    private void loadAlbumSongs(Song song) {
        albumSongs.clear();
        if (song != null && song.album != null && !song.album.isEmpty()) {
            for (Song s : queue) {
                if (s != null && song.album.equals(s.album)) {
                    addSongUnique(albumSongs, s);
                }
            }
        }
        tvAlbumTitle.setText(song != null && song.album != null && !song.album.isEmpty()
                ? song.album : "专辑信息");
        albumAdapter.notifyDataSetChanged();

        // 有 albumId 时异步拉取完整专辑歌曲
        if (song != null && song.albumId != null && !song.albumId.isEmpty()) {
            new Thread(() -> {
                try {
                    SourceProvider provider = SourceManager.getInstance().getProviderByType(song.sourceType);
                    if (provider instanceof PublicSourceAdapter) {
                        List<Song> tracks = ((PublicSourceAdapter) provider).getAlbumTracks(song.albumId, song.album);
                        runOnUiThread(() -> {
                            if (tracks != null && !tracks.isEmpty()) {
                                albumSongs.clear();
                                for (Song s : tracks) addSongUnique(albumSongs, s);
                                // 当前歌曲置顶高亮
                                albumAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                } catch (Exception ignored) {
                }
            }).start();
        }
    }

    private void addSongUnique(List<Song> list, Song s) {
        for (Song exist : list) {
            if (exist.id != null && exist.id.equals(s.id)) return;
        }
        list.add(s);
    }

    /**
     * 切换 歌词 / 专辑 视图
     */
    private void switchMode(int mode) {
        viewMode = mode;
        boolean isLyric = mode == MODE_LYRIC;
        rvLyric.setVisibility(isLyric ? View.VISIBLE : View.GONE);
        albumContainer.setVisibility(isLyric ? View.GONE : View.VISIBLE);
        tabLyric.setSelected(isLyric);
        tabAlbum.setSelected(!isLyric);
        tabLyric.setTextColor(ContextCompat.getColor(this, isLyric ? R.color.text_accent : R.color.text_secondary));
        tabAlbum.setTextColor(ContextCompat.getColor(this, isLyric ? R.color.text_secondary : R.color.text_accent));
        if (!isLyric && !albumSongs.isEmpty()) {
            rvAlbumSongs.smoothScrollToPosition(0);
        }
    }

    private void togglePlay() {
        MusicPlaybackService service = getServiceInstance();
        if (service == null || currentSong == null) return;

        if (isPlaying) {
            service.pause();
            isPlaying = false;
            ivPlayPause.setImageResource(R.drawable.ic_play);
        } else {
            service.resume();
            isPlaying = true;
            ivPlayPause.setImageResource(R.drawable.ic_pause);
        }
    }

    private void skipNext() {
        if (queue.size() <= 1) {
            Toast.makeText(this, "当前无播放列表，无法切歌", Toast.LENGTH_SHORT).show();
            return;
        }
        switchTo(currentIndex + 1);
    }

    private void skipPrev() {
        if (queue.size() <= 1) {
            Toast.makeText(this, "当前无播放列表，无法切歌", Toast.LENGTH_SHORT).show();
            return;
        }
        switchTo(currentIndex - 1);
    }

    private void updateProgress() {
        MusicPlaybackService service = getServiceInstance();
        if (service == null) return;
        long current = service.getCurrentPosition();
        long total = service.getDuration();
        if (total > 0) {
            progressBar.setProgress((int) (current * 100 / total));
            tvCurrentTime.setText(formatDuration(current));
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (tabLyric.hasFocus()) {
                    switchMode(MODE_LYRIC);
                    return true;
                }
                if (tabAlbum.hasFocus()) {
                    switchMode(MODE_ALBUM);
                    return true;
                }
                if (ivPlayPause.isFocused()) {
                    togglePlay();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (ivPlayPause.hasFocus() || ivNext.hasFocus() || ivPrev.hasFocus()) {
                    if (ivPlayPause.hasFocus()) {
                        ivPrev.requestFocus();
                        return true;
                    } else if (ivNext.hasFocus()) {
                        ivPlayPause.requestFocus();
                        return true;
                    }
                } else {
                    // 专辑模式切回歌词
                    if (viewMode == MODE_ALBUM) switchMode(MODE_LYRIC);
                    tabLyric.requestFocus();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (ivPlayPause.hasFocus() || ivNext.hasFocus() || ivPrev.hasFocus()) {
                    if (ivPlayPause.hasFocus()) {
                        ivNext.requestFocus();
                        return true;
                    } else if (ivPrev.hasFocus()) {
                        ivPlayPause.requestFocus();
                        return true;
                    }
                } else {
                    // 歌词模式切到专辑
                    if (viewMode == MODE_LYRIC) switchMode(MODE_ALBUM);
                    tabAlbum.requestFocus();
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setupFocus(View view) {
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.15f : 1.0f;
            v.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(200)
                .start();
            v.setAlpha(hasFocus ? 1.0f : 0.7f);
        });
    }

    private void setupTabFocus(View tab) {
        tab.setFocusable(true);
        tab.setFocusableInTouchMode(true);
        tab.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.1f : 1.0f;
            v.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(200)
                .start();
        });
    }

    private MusicPlaybackService getServiceInstance() {
        return MusicPlaybackService.getInstance();
    }

    private String formatDuration(long ms) {
        if (ms <= 0) return "00:00";
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressHandler != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    // ==================== 多行歌词 Adapter ====================

    private class LyricAdapter extends RecyclerView.Adapter<LyricHolder> {
        private final List<LyricLine> lines;

        LyricAdapter(List<LyricLine> lines) {
            this.lines = lines;
        }

        @Override
        public LyricHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_lyric, parent, false);
            return new LyricHolder(v);
        }

        @Override
        public void onBindViewHolder(LyricHolder holder, int position) {
            LyricLine line = lines.get(position);
            boolean active = position == activeLyricIndex;
            holder.tvLyric.setText(line.text);
            holder.tvLyric.setTextColor(ContextCompat.getColor(PlayerActivity.this,
                    active ? R.color.text_accent : R.color.text_secondary));
            holder.tvLyric.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, active ? 30 : 21);
            holder.tvLyric.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }

        @Override
        public int getItemCount() {
            return lines.size();
        }
    }

    private static class LyricHolder extends RecyclerView.ViewHolder {
        final TextView tvLyric;

        LyricHolder(View itemView) {
            super(itemView);
            tvLyric = (TextView) itemView;
        }
    }

    // ==================== 专辑歌曲 Adapter ====================

    private class AlbumSongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Song> songs;

        AlbumSongAdapter(List<Song> songs) {
            this.songs = songs;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_song, parent, false);
            return new SongHolder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Song song = songs.get(position);
            SongHolder sh = (SongHolder) holder;

            sh.tvTitle.setText(song.title);
            sh.tvArtist.setText(song.artist);
            if (song.album != null && !song.album.isEmpty()) {
                sh.tvAlbum.setText(song.album);
                sh.tvAlbum.setVisibility(View.VISIBLE);
            } else {
                sh.tvAlbum.setVisibility(View.GONE);
            }
            sh.tvDuration.setText(formatDuration(song.duration));
            sh.tvSource.setText("");

            boolean isCurrent = currentSong != null && song.id != null && song.id.equals(currentSong.id);
            sh.tvTitle.setTextColor(ContextCompat.getColor(PlayerActivity.this,
                    isCurrent ? R.color.text_accent : R.color.text_primary));
            sh.tvTitle.setTypeface(null, isCurrent ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

            if (song.coverUrl != null && !song.coverUrl.isEmpty()) {
                Glide.with(PlayerActivity.this)
                    .load(song.coverUrl)
                    .placeholder(R.drawable.placeholder_cover)
                    .error(R.drawable.placeholder_cover)
                    .centerCrop()
                    .into(sh.ivCover);
            }

            sh.itemView.setOnClickListener(v -> {
                int idx = findIndexById(song);
                if (idx >= 0) {
                    switchTo(idx);
                } else {
                    queue.add(song);
                    switchTo(queue.size() - 1);
                }
            });
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }
    }

    private static class SongHolder extends RecyclerView.ViewHolder {
        final ImageView ivCover;
        final TextView tvTitle;
        final TextView tvArtist;
        final TextView tvAlbum;
        final TextView tvSource;
        final TextView tvDuration;

        SongHolder(View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtist = itemView.findViewById(R.id.tv_artist);
            tvAlbum = itemView.findViewById(R.id.tv_album);
            tvSource = itemView.findViewById(R.id.tv_source);
            tvDuration = itemView.findViewById(R.id.tv_duration);
        }
    }

    /**
     * 歌词行数据结构：时间毫秒 + 文本
     */
    private static class LyricLine {
        long time;
        String text;

        static LyricLine parse(String line) {
            int idx = line.indexOf(']');
            if (idx <= 1) return null;
            String timePart = line.substring(1, idx);
            String text = line.substring(idx + 1).trim();
            if (text.isEmpty()) return null;
            long ms = 0;
            String[] parts = timePart.split(":");
            try {
                if (parts.length >= 2) {
                    ms = (long) (Double.parseDouble(parts[0]) * 60 * 1000
                            + Double.parseDouble(parts[1]) * 1000);
                } else {
                    ms = (long) (Double.parseDouble(parts[0]) * 1000);
                }
            } catch (NumberFormatException e) {
                return null;
            }
            LyricLine ll = new LyricLine();
            ll.time = ms;
            ll.text = text;
            return ll;
        }
    }
}