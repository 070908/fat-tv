package com.fattv.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.fattv.app.R;
import com.fattv.app.model.Song;
import com.fattv.app.player.MusicPlaybackService;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * PlayerActivity - TV 播放器页：金色主题、圆角封面、进度条、矢量图标控制
 */
public class PlayerActivity extends AppCompatActivity {
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private ImageView ivCover, ivPlayPause, ivNext, ivPrev, bgBlur;
    private ProgressBar progressBar;
    private Song currentSong;
    private boolean isPlaying = false;
    private android.os.Handler progressHandler;
    private Runnable progressRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // 绑定视图
        tvTitle = findViewById(R.id.tv_title);
        tvArtist = findViewById(R.id.tv_artist);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        ivCover = findViewById(R.id.iv_cover);
        ivPlayPause = findViewById(R.id.btn_play_pause);
        ivNext = findViewById(R.id.btn_next);
        ivPrev = findViewById(R.id.btn_prev);
        bgBlur = findViewById(R.id.bg_blur);
        progressBar = findViewById(R.id.progress_bar);

        // 焦点设置
        setupFocus(ivPlayPause);
        setupFocus(ivNext);
        setupFocus(ivPrev);

        // 初始化进度条更新
        progressHandler = new android.os.Handler();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                updateProgress();
                progressHandler.postDelayed(this, 1000);
            }
        };

        // 加载歌曲
        currentSong = (Song) getIntent().getSerializableExtra("song");
        if (currentSong != null) {
            bindSongInfo(currentSong);
            playCurrentSong();
        }

        // 点击事件
        ivPlayPause.setOnClickListener(v -> togglePlay());
        ivNext.setOnClickListener(v -> skipNext());
        ivPrev.setOnClickListener(v -> skipPrev());
    }

    private void bindSongInfo(Song song) {
        tvTitle.setText(song.title);
        tvArtist.setText(song.artist + (song.album != null && !song.album.isEmpty() ? " / " + song.album : ""));
        tvTotalTime.setText(formatDuration(song.duration));

        // 加载封面图到主封面和背景
        if (song.coverUrl != null && !song.coverUrl.isEmpty()) {
            Glide.with(this)
                .load(song.coverUrl)
                .placeholder(R.drawable.ic_logo)
                .error(R.drawable.ic_logo)
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .centerCrop()
                .into(ivCover);

            // 背景模糊图（同一张图，低 alpha 营造氛围）
            Glide.with(this)
                .load(song.coverUrl)
                .placeholder(R.drawable.bg_main)
                .error(R.drawable.bg_main)
                .centerCrop()
                .into(bgBlur);
        }
    }

    private void playCurrentSong() {
        Intent serviceIntent = new Intent(this, MusicPlaybackService.class);
        startService(serviceIntent);
        new android.os.Handler().postDelayed(() -> {
            MusicPlaybackService service = getServiceInstance();
            if (service != null) {
                service.playSong(currentSong);
                isPlaying = true;
                ivPlayPause.setImageResource(R.drawable.ic_pause);
                progressHandler.post(progressRunnable);
            }
        }, 500);
    }

    private void togglePlay() {
        MusicPlaybackService service = getServiceInstance();
        if (service == null || currentSong == null) return;

        if (isPlaying) {
            service.pause();
            isPlaying = false;
            ivPlayPause.setImageResource(R.drawable.ic_play);
            progressHandler.removeCallbacks(progressRunnable);
        } else {
            service.resume();
            isPlaying = true;
            ivPlayPause.setImageResource(R.drawable.ic_pause);
            progressHandler.post(progressRunnable);
        }
    }

    private void skipNext() {
        // TODO: 播放列表下一首
    }

    private void skipPrev() {
        // TODO: 播放列表上一首
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
                if (ivPlayPause.isFocused()) {
                    togglePlay();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (ivPlayPause.isFocused()) {
                    ivPrev.requestFocus();
                    return true;
                } else if (ivNext.isFocused()) {
                    ivPlayPause.requestFocus();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (ivPlayPause.isFocused()) {
                    ivNext.requestFocus();
                    return true;
                } else if (ivPrev.isFocused()) {
                    ivPlayPause.requestFocus();
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
}
