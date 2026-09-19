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
import com.fattv.app.ui.fragment.TranslucentFragment;

/**
 * MainActivity - TV 主界面：顶部导航栏 + Fragment 内容区
 * 参考 algerkong AlgerMusicPlayer 风格：顶部 Tab 栏（首页/歌单/专辑/排行榜/MV/本地音乐）
 * 全局黑金海绵宝宝背景 + 各页面半透明面板。
 * <p>
 * DPad 焦点流转规则（严谨设计）：
 * <ul>
 *   <li>焦点在 Tab 栏：左右键切换 Tab，确认键进入该 Tab，下键落入内容区首个可聚焦元素</li>
 *   <li>焦点在内容区：上键回到 Tab 栏；其余空格键交由 Fragment 内控件处理</li>
 *   <li>每次切换 Tab 后焦点自动回到对应 Tab，避免焦点丢失</li>
 * </ul>
 */
public class MainActivity extends AppCompatActivity {

    private static final int TAB_COUNT = 6;
    private static final int NAV_COUNT = TAB_COUNT + 2; // + search + settings
    private static final int IDX_SEARCH = TAB_COUNT;
    private static final int IDX_SETTINGS = TAB_COUNT + 1;
    private static final int[] TAB_IDS = {
            R.id.tab_home, R.id.tab_playlist, R.id.tab_album,
            R.id.tab_rank, R.id.tab_mv, R.id.tab_local
    };
    private static final Class<? extends Fragment>[] TAB_FRAGMENTS = new Class[]{
            HomeFragment.class, PlaylistFragment.class, AlbumFragment.class,
            RankFragment.class, MvFragment.class, LocalMusicFragment.class
    };

    private TextView[] tabViews = new TextView[TAB_COUNT];
    private View[] topNavViews = new View[NAV_COUNT];
    private int currentTab = 0;
    private Fragment[] fragmentInstances = new Fragment[TAB_COUNT];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SourceManager.getInstance().init(this);
        SourceManager.getInstance().checkAllHealth();

        // 绑定 Tab 按钮并填充顶部导航数组
        for (int i = 0; i < TAB_COUNT; i++) {
            tabViews[i] = findViewById(TAB_IDS[i]);
            topNavViews[i] = tabViews[i];
            final int index = i;
            tabViews[i].setOnClickListener(v -> switchToTab(index));
            setupTabFocus(tabViews[i], i);
        }

        // 绑定导航栏搜索/设置按钮
        ImageView btnNavSearch = findViewById(R.id.btn_nav_search);
        ImageView btnNavSettings = findViewById(R.id.btn_nav_settings);
        topNavViews[IDX_SEARCH] = btnNavSearch;
        topNavViews[IDX_SETTINGS] = btnNavSettings;
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

        // 默认显示首页并将焦点落在首页 Tab
        switchToTab(0);
        tabViews[0].requestFocus();
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
        if (currentTab == index && fragmentInstances[index] != null) {
            // 已有实例：仅刷新视觉态
            updateTabVisuals(index);
            return;
        }

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

    /**
     * 当前焦点是否落在顶部导航区（6 Tab + 搜索 + 设置）
     */
    private boolean isTopNavFocused() {
        View focused = getCurrentFocus();
        if (focused == null) return false;
        for (View v : topNavViews) {
            if (focused == v) return true;
        }
        return false;
    }

    /**
     * 计算焦点在顶部导航区的索引
     */
    private int findTopNavIndex(View view) {
        for (int i = 0; i < NAV_COUNT; i++) {
            if (view == topNavViews[i]) return i;
        }
        return -1;
    }

    /**
     * 将焦点移动到顶部导航区的指定位置，同时切换 Fragment（Tab 范围内）
     */
    private void moveTopNavFocus(int toIndex) {
        if (toIndex < 0) toIndex = NAV_COUNT - 1;
        if (toIndex >= NAV_COUNT) toIndex = 0;
        // 先同步切换 Fragment（commitNow），避免 Fragment 布局变更抢走焦点；
        // 再请求 Tab 焦点，保证左右移动后焦点稳定留在顶部导航环上
        if (toIndex < TAB_COUNT) {
            switchToTab(toIndex);
        }
        topNavViews[toIndex].requestFocus();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean onTopNav = isTopNavFocused();

        if (onTopNav) {
            View focused = getCurrentFocus();
            int idx = findTopNavIndex(focused);

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                // 水平环：向左移动焦点
                moveTopNavFocus(idx - 1);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                // 水平环：向右移动焦点
                moveTopNavFocus(idx + 1);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                // 从顶部导航向下进入当前 Fragment 内容区第一个可聚焦元素
                // 若焦点在搜索/设置上，也落到当前 Tab 的内容区
                Fragment current = fragmentInstances[currentTab];
                if (current instanceof TranslucentFragment) {
                    ((TranslucentFragment) current).requestContentFocus();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                // 确认键：若焦点落在 Tab 上则切换 Fragment
                if (idx >= 0 && idx < TAB_COUNT) {
                    switchToTab(idx);
                    return true;
                }
                // 搜索/设置图标走 onClick 点击事件
                return super.onKeyDown(keyCode, event);
            }
        } else {
            // 焦点在内容区：上键优先回到「返回」按钮（歌单/专辑曲目态），再上才回 Tab
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                View focused = getCurrentFocus();
                Fragment current = fragmentInstances[currentTab];
                if (current != null && current.getView() != null) {
                    View btnBack = current.getView().findViewById(R.id.btn_back_album);
                    if (btnBack != null && btnBack.getVisibility() == View.VISIBLE && focused != btnBack) {
                        btnBack.requestFocus();
                        return true;
                    }
                }
                tabViews[currentTab].requestFocus();
                updateTabVisuals(currentTab);
                return true;
            }
        }
        // 其余按键交由系统/内容区处理
        return super.onKeyDown(keyCode, event);
    }
}