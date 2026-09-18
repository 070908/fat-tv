package com.fattv.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fattv.app.R;
import com.fattv.app.source.SourceManager;
import com.fattv.app.ui.fragment.AlbumFragment;
import com.fattv.app.ui.fragment.HomeFragment;
import com.fattv.app.ui.fragment.LocalMusicFragment;
import com.fattv.app.ui.fragment.MvFragment;
import com.fattv.app.ui.fragment.PlaylistFragment;
import com.fattv.app.ui.fragment.RankFragment;

/**
 * MainActivity - TV 主界面：顶部导航栏 + Fragment 内容区
 * 参考 algerkong AlgerMusicPlayer 风格：顶部 Tab 栏（首页/歌单/专辑/排行榜/MV/本地音乐）
 * 全局黑金海绵宝宝背景 + 各页面半透明面板
 */
public class MainActivity extends AppCompatActivity {

    private static final int TAB_COUNT = 6;
    private static final int[] TAB_IDS = {
            R.id.tab_home, R.id.tab_playlist, R.id.tab_album,
            R.id.tab_rank, R.id.tab_mv, R.id.tab_local
    };
    private static final Class<? extends Fragment>[] TAB_FRAGMENTS = new Class[]{
            HomeFragment.class, PlaylistFragment.class, AlbumFragment.class,
            RankFragment.class, MvFragment.class, LocalMusicFragment.class
    };

    private TextView[] tabViews = new TextView[TAB_COUNT];
    private int currentTab = 0;
    private Fragment[] fragmentInstances = new Fragment[TAB_COUNT];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SourceManager.getInstance().init(this);
        SourceManager.getInstance().checkAllHealth();

        // 绑定 Tab 按钮
        for (int i = 0; i < TAB_COUNT; i++) {
            tabViews[i] = findViewById(TAB_IDS[i]);
            final int index = i;
            tabViews[i].setOnClickListener(v -> switchToTab(index));
            setupTabFocus(tabViews[i], i);
        }

        // 绑定导航栏搜索/设置按钮
        ImageView btnNavSearch = findViewById(R.id.btn_nav_search);
        ImageView btnNavSettings = findViewById(R.id.btn_nav_settings);
        btnNavSearch.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        });
        btnNavSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        setupIconFocus(btnNavSearch);
        setupIconFocus(btnNavSettings);

        // 默认显示首页
        switchToTab(0);
    }

    private void setupIconFocus(ImageView icon) {
        icon.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.2f : 1.0f;
            v.animate().scaleX(scale).scaleY(scale).setDuration(150).start();
        });
    }

    /**
     * 切换到指定 Tab（支持外部调用，如 Fragment 内跳转）
     */
    public void switchToTab(int index) {
        if (index < 0 || index >= TAB_COUNT) return;
        if (currentTab == index && fragmentInstances[index] != null) return;

        // 更新 Tab 视觉状态
        updateTabVisuals(index);

        // Fragment 切换
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        // 隐藏当前 Fragment
        if (fragmentInstances[currentTab] != null) {
            ft.hide(fragmentInstances[currentTab]);
        }

        // 显示/创建目标 Fragment
        if (fragmentInstances[index] == null) {
            try {
                fragmentInstances[index] = TAB_FRAGMENTS[index].newInstance();
                ft.add(R.id.fragment_container, fragmentInstances[index], "tab_" + index);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else {
            ft.show(fragmentInstances[index]);
        }

        ft.commitNowAllowingStateLoss();
        currentTab = index;
    }

    private void updateTabVisuals(int selectedIndex) {
        for (int i = 0; i < TAB_COUNT; i++) {
            boolean selected = (i == selectedIndex);
            tabViews[i].setSelected(selected);
            tabViews[i].setTextColor(getResources().getColor(
                    selected ? R.color.text_primary : R.color.text_secondary));
        }
    }

    private void setupTabFocus(TextView tab, int index) {
        tab.setOnFocusChangeListener((v, hasFocus) -> {
            TextView tv = (TextView) v;
            if (hasFocus) {
                // 焦点在 Tab 上时高亮，但不自动切换（等用户按确认）
                tv.setTextColor(getResources().getColor(R.color.text_accent));
            } else {
                boolean selected = (index == currentTab);
                tv.setTextColor(getResources().getColor(
                        selected ? R.color.text_primary : R.color.text_secondary));
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // DPad 左右切换 Tab
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (currentTab > 0) {
                switchToTab(currentTab - 1);
                tabViews[currentTab].requestFocus();
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (currentTab < TAB_COUNT - 1) {
                switchToTab(currentTab + 1);
                tabViews[currentTab].requestFocus();
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            // 从 Tab 栏向下进入 Fragment 内容区
            Fragment current = fragmentInstances[currentTab];
            if (current != null && current.getView() != null) {
                View focusable = current.getView().findFocus();
                if (focusable == null) {
                    // 尝试让内容区第一个可聚焦元素获得焦点
                    View content = current.getView().findViewById(R.id.rv_recent);
                    if (content == null) content = current.getView().findViewById(R.id.rv_list);
                    if (content == null) content = current.getView().findViewById(R.id.tv_page_title);
                    if (content != null) {
                        content.requestFocus();
                        return true;
                    }
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            // 从内容区向上回到 Tab 栏
            tabViews[currentTab].requestFocus();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            // 确认键切换 Tab
            View focused = getCurrentFocus();
            for (int i = 0; i < TAB_COUNT; i++) {
                if (tabViews[i] == focused) {
                    switchToTab(i);
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
