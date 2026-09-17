package com.fattv.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.fattv.app.R;
import com.fattv.app.model.Song;
import com.fattv.app.player.MusicPlaybackService;
import com.fattv.app.source.SourceManager;

/**
 * PlayerActivity - TV 播放器页：封面、歌曲信息、播放控制（DPad 操作）
 */
public class PlayerActivity extends AppCompatActivity {
    private TextView tvTitle, tvArtist;
    private ImageView ivPlayPause, ivNext, ivPrev;
    private Song currentSong;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        tvTitle = findViewById(R.id.tv_title);
        tvArtist = findViewById(R.id.tv_artist);
        ivPlayPause = findViewById(R.id.btn_play_pause);
        ivNext = findViewById(R.id.btn_next);
        ivPrev = findViewById(R.id.btn_prev);

        setupFocus(ivPlayPause);
        setupFocus(ivNext);
        setupFocus(ivPrev);

        currentSong = (Song) getIntent().getSerializableExtra("song");
        if (currentSong != null) {
            tvTitle.setText(currentSong.title);
            tvArtist.setText(currentSong.artist);
            playCurrentSong();
        }

        ivPlayPause.setOnClickListener(v -> togglePlay());
    }

    private void playCurrentSong() {
        Intent serviceIntent = new Intent(this, MusicPlaybackService.class);
        startService(serviceIntent);
        // 延迟等待服务初始化后发送播放指令
        new android.os.Handler().postDelayed(() -> {
            MusicPlaybackService service = getServiceInstance();
            if (service != null) service.playSong(currentSong);
            isPlaying = true;
            ivPlayPause.setImageResource(R.drawable.ic_logo);
        }, 500);
    }

    private void togglePlay() {
        // 简化版：重新开始播放
        if (currentSong != null) playCurrentSong();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                togglePlay();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setupFocus(View view) {
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            v.setScaleX(hasFocus ? 1.15f : 1.0f);
            v.setScaleY(hasFocus ? 1.15f : 1.0f);
            v.setAlpha(hasFocus ? 1.0f : 0.6f);
        });
    }

    private MusicPlaybackService getServiceInstance() {
        return MusicPlaybackService.getInstance();
    }
}
