package com.fattv.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fattv.app.R;
import com.fattv.app.model.Song;
import com.fattv.app.source.PublicSourceAdapter;
import com.fattv.app.source.SourceManager;
import com.fattv.app.source.SourceProvider;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

/**
 * MvPlayerActivity - MV 视频播放页
 * 黑金主题、SurfaceView 播放、DPad 返回
 */
public class MvPlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private SurfaceView svVideo;
    private ProgressBar progressLoading;
    private ImageView btnBack;
    private TextView tvTitle, tvArtist;
    private Song mv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mv_player);

        svVideo = findViewById(R.id.sv_video);
        progressLoading = findViewById(R.id.progress_loading);
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        tvArtist = findViewById(R.id.tv_artist);

        mv = (Song) getIntent().getSerializableExtra("song");
        if (mv == null) {
            Toast.makeText(this, "播放失败：缺少MV信息", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle.setText(mv.title);
        tvArtist.setText(mv.artist);

        btnBack.setOnClickListener(v -> finish());
        btnBack.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.2f : 1.0f;
            v.animate().scaleX(scale).scaleY(scale).setDuration(150).start();
        });

        player = new ExoPlayer.Builder(this).build();
        svVideo.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                player.setVideoSurfaceView(svVideo);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });

        loadAndPlay();
    }

    private void loadAndPlay() {
        progressLoading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                SourceProvider provider = SourceManager.getInstance().getProviderByType(SourceProvider.SourceType.PUBLIC);
                String url = "";
                if (provider instanceof PublicSourceAdapter) {
                    url = ((PublicSourceAdapter) provider).resolveMvUrl(mv.id);
                }
                final String finalUrl = url;
                runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    if (finalUrl == null || finalUrl.isEmpty()) {
                        Toast.makeText(this, "MV 地址获取失败，请检查网络或音源", Toast.LENGTH_LONG).show();
                        return;
                    }
                    player.setMediaItem(MediaItem.fromUri(finalUrl));
                    player.prepare();
                    player.play();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "MV 加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK
                || keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN && btnBack.isFocused()
                || keyCode == android.view.KeyEvent.KEYCODE_ESCAPE) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}