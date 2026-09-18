package com.fattv.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.fattv.app.R;
import com.fattv.app.source.SourceManager;

/**
 * MainActivity - TV 首页：搜索、播放列表、设置入口 + 半透明背景
 */
public class MainActivity extends AppCompatActivity {

    private View btnSearch, btnPlaylist, btnSettings, btnSourceSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SourceManager.getInstance().init(this);
        SourceManager.getInstance().checkAllHealth();

        ImageView bg = findViewById(R.id.bg_image);
        bg.setImageResource(R.drawable.bg_main);

        btnSearch = findViewById(R.id.btn_search);
        btnPlaylist = findViewById(R.id.btn_playlist);
        btnSettings = findViewById(R.id.btn_settings);
        btnSourceSwitch = findViewById(R.id.btn_source_switch);

        setupFocus(btnSearch);
        setupFocus(btnPlaylist);
        setupFocus(btnSettings);
        setupFocus(btnSourceSwitch);

        btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnSourceSwitch.setOnClickListener(v -> {
            if (!SourceManager.getInstance().isCurrentHealthy()) {
                SourceManager.getInstance().autoFallback();
            }
        });
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
}
