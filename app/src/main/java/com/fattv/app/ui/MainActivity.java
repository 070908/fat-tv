package com.fattv.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.fattv.app.R;
import com.fattv.app.source.SourceManager;
import com.fattv.app.source.SourceProvider;

import java.util.List;

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
        btnPlaylist.setOnClickListener(v -> startActivity(new Intent(this, PlaylistActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnSourceSwitch.setOnClickListener(v -> showSourcePickerDialog());
    }

    /**
     * 音源切换：弹出当前已注册音源列表，选中即切换并提示结果
     */
    private void showSourcePickerDialog() {
        SourceManager sm = SourceManager.getInstance();
        sm.checkAllHealth();
        List<SourceProvider> providers = sm.getAllProviders();
        if (providers == null || providers.isEmpty()) {
            Toast.makeText(this, "\u65e0\u53ef\u7528\u97f3\u6e90", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] names = new String[providers.size()];
        int checked = -1;
        for (int i = 0; i < providers.size(); i++) {
            SourceProvider p = providers.get(i);
            names[i] = p.getName();
            if (p == sm.getCurrentProvider()) {
                checked = i;
            }
        }

        new AlertDialog.Builder(this)
            .setTitle(R.string.switch_source)
            .setSingleChoiceItems(names, checked, null)
            .setPositiveButton("\u786e\u5b9a", (dialog, which) -> {
                int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                if (selected < 0 || selected >= providers.size()) {
                    Toast.makeText(this, "\u8bf7\u5148\u9009\u62e9\u97f3\u6e90", Toast.LENGTH_SHORT).show();
                    return;
                }
                SourceProvider target = providers.get(selected);
                sm.setSourceByType(target.getType());
                sm.checkAllHealth();
                String health = sm.isCurrentHealthy() ? "\u53ef\u7528" : "\u4e0d\u53ef\u7528";
                Toast.makeText(this, "\u5df2\u5207\u6362\u5230: " + target.getName() + " (" + health + ")", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("\u53d6\u6d88", null)
            .show();
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
