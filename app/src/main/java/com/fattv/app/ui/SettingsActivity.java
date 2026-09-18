package com.fattv.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fattv.app.R;
import com.fattv.app.source.SourceManager;
import com.fattv.app.source.SourceProvider;

/**
 * SettingsActivity - TV 设置页：音源切换、局域网后端配置、第三方音源导入
 */
public class SettingsActivity extends AppCompatActivity {
    private RadioGroup rgSource;
    private EditText etLocalUrl, etPluginUrl;
    private View btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        rgSource = findViewById(R.id.rg_source);
        etLocalUrl = findViewById(R.id.et_local_url);
        etPluginUrl = findViewById(R.id.et_plugin_url);
        btnSave = findViewById(R.id.btn_save);

        // 加载当前设置
        etLocalUrl.setText(SourceManager.getInstance().getLocalServerUrl());
        etPluginUrl.setText(SourceManager.getInstance().getPublicPluginUrl());

        rgSource.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_public) {
                SourceManager.getInstance().setSourceByType(SourceProvider.SourceType.PUBLIC);
            } else if (checkedId == R.id.rb_local) {
                SourceManager.getInstance().setSourceByType(SourceProvider.SourceType.LOCAL);
            }
        });

        btnSave.setOnClickListener(v -> saveSettings());
        setupFocus(btnSave);
        btnSave.requestFocus();
    }

    private void saveSettings() {
        SourceManager.getInstance().setLocalServerUrl(etLocalUrl.getText().toString().trim());
        SourceManager.getInstance().setPublicPluginUrl(etPluginUrl.getText().toString().trim());

        // 检测当前源健康状态
        SourceManager.getInstance().checkAllHealth();
        if (!SourceManager.getInstance().isCurrentHealthy()) {
            new AlertDialog.Builder(this)
                .setTitle("\u97f3\u6e90\u5931\u6548")
                .setMessage("\u5f53\u524d\u97f3\u6e90\u65e0\u6cd5\u8bbf\u95ee\uff0c\u662f\u5426\u5207\u6362\u5230\u5907\u7528\u97f3\u6e90\uff1f")
                .setPositiveButton("\u81ea\u52a8\u5207\u6362", (dialog, which) -> {
                    if (SourceManager.getInstance().autoFallback()) {
                        Toast.makeText(this, "\u5df2\u5207\u6362\u97f3\u6e90", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("\u4fdd\u6301\u5f53\u524d", null)
                .show();
        } else {
            Toast.makeText(this, "\u8bbe\u7f6e\u5df2\u4fdd\u5b58", Toast.LENGTH_SHORT).show();
        }
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
