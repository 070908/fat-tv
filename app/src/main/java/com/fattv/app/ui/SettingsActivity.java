package com.fattv.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fattv.app.R;
import com.fattv.app.source.SourceManager;
import com.fattv.app.source.SourceProvider;

/**
 * SettingsActivity - TV 设置页（algerkong 风格）
 * 三 Tab：音源选择 / 落雪音源 / 自定义API
 * 含网络代理配置
 */
public class SettingsActivity extends AppCompatActivity {

    // Tab
    private TextView tabSourceSelect, tabLxSource, tabCustomApi;
    // 面板
    private View panelSourceSelect, panelLxSource, panelCustomApi;
    // 音源选择面板控件
    private RadioGroup rgSource;
    private EditText etProxyType, etProxyHost, etProxyPort;
    // 落雪音源面板控件
    private EditText etLxUrl;
    // 自定义API面板控件
    private EditText etCustomApiUrl;
    // 底部按钮
    private View btnSave, btnCancel;

    private int currentTab = 0;
    private static final int TAB_COUNT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        bindViews();
        setupTabs();
        loadSettings();
        setupActions();

        switchTab(0);
        tabSourceSelect.requestFocus();
    }

    private void bindViews() {
        // Tab
        tabSourceSelect = findViewById(R.id.tab_source_select);
        tabLxSource = findViewById(R.id.tab_lx_source);
        tabCustomApi = findViewById(R.id.tab_custom_api);
        // 面板
        panelSourceSelect = findViewById(R.id.panel_source_select);
        panelLxSource = findViewById(R.id.panel_lx_source);
        panelCustomApi = findViewById(R.id.panel_custom_api);
        // 音源选择
        rgSource = findViewById(R.id.rg_source);
        etProxyType = findViewById(R.id.et_proxy_type);
        etProxyHost = findViewById(R.id.et_proxy_host);
        etProxyPort = findViewById(R.id.et_proxy_port);
        // 落雪音源
        etLxUrl = findViewById(R.id.et_lx_url);
        // 自定义API
        etCustomApiUrl = findViewById(R.id.et_custom_api_url);
        // 底部按钮
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
    }

    private void setupTabs() {
        TextView[] tabs = {tabSourceSelect, tabLxSource, tabCustomApi};
        for (int i = 0; i < TAB_COUNT; i++) {
            final int index = i;
            tabs[i].setOnClickListener(v -> switchTab(index));
            setupFocus(tabs[i]);
        }
    }

    private void switchTab(int index) {
        currentTab = index;
        // 更新 Tab 视觉
        tabSourceSelect.setTextColor(getResources().getColor(index == 0 ? R.color.text_accent : R.color.text_secondary));
        tabSourceSelect.setTypeface(null, index == 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabLxSource.setTypeface(null, index == 1 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabCustomApi.setTypeface(null, index == 2 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        // 显示对应面板
        panelSourceSelect.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        panelLxSource.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        panelCustomApi.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
    }

    private void loadSettings() {
        // 音源选择
        SourceProvider current = SourceManager.getInstance().getCurrentProvider();
        if (current != null && current.getType() == SourceProvider.SourceType.LOCAL) {
            rgSource.check(R.id.rb_local);
        } else {
            rgSource.check(R.id.rb_public);
        }
        // 代理设置（从 SharedPreferences 读取）
        etProxyType.setText(SourceManager.getInstance().getProxyType());
        etProxyHost.setText(SourceManager.getInstance().getProxyHost());
        etProxyPort.setText(SourceManager.getInstance().getProxyPort());
    }

    private void setupActions() {
        rgSource.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_public) {
                SourceManager.getInstance().setSourceByType(SourceProvider.SourceType.PUBLIC);
            } else if (checkedId == R.id.rb_local) {
                SourceManager.getInstance().setSourceByType(SourceProvider.SourceType.LOCAL);
            }
        });

        btnSave.setOnClickListener(v -> saveSettings());
        btnCancel.setOnClickListener(v -> finish());

        setupFocus(btnSave);
        setupFocus(btnCancel);
    }

    private void saveSettings() {
        // 保存代理设置
        SourceManager.getInstance().setProxyConfig(
            etProxyType.getText().toString().trim(),
            etProxyHost.getText().toString().trim(),
            etProxyPort.getText().toString().trim()
        );

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
            finish();
        }
    }

    private void setupFocus(View view) {
        if (view instanceof TextView) {
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
        }
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
