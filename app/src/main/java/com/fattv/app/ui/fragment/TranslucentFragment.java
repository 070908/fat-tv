package com.fattv.app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fattv.app.R;

/**
 * TranslucentFragment - TV 半透明 Fragment 基类
 * 所有 Tab 页面继承此类，自动应用全局海绵宝宝背景 + 半透明内容面板
 */
public abstract class TranslucentFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 根布局：外层 FrameLayout 承载全局背景
        FrameLayout root = new FrameLayout(requireContext());
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // 全局背景层（海绵宝宝 bg_main，alpha 0.35）
        View bgView = new View(requireContext());
        bgView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        bgView.setBackgroundResource(R.drawable.bg_main);
        bgView.setAlpha(0.35f);
        root.addView(bgView);

        // 暗色遮罩，确保内容可读
        View overlay = new View(requireContext());
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(getResources().getColor(R.color.bg_overlay));
        root.addView(overlay);

        // 半透明内容面板：子类实现 createContentView()
        View contentView = createContentView(inflater, container, savedInstanceState);
        if (contentView != null) {
            // 内容区自动加半透明背景
            contentView.setBackgroundResource(R.color.bg_surface);
            // 子类布局如果已有背景会覆盖，这里主要是兜底
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            lp.setMargins(
                    getResources().getDimensionPixelSize(R.dimen.tv_safe_margin),
                    getResources().getDimensionPixelSize(R.dimen.tv_safe_margin) + 120, // 顶部留出导航栏高度
                    getResources().getDimensionPixelSize(R.dimen.tv_safe_margin),
                    getResources().getDimensionPixelSize(R.dimen.tv_safe_margin)
            );
            contentView.setLayoutParams(lp);
            root.addView(contentView);
        }

        return root;
    }

    /**
     * 子类实现：创建实际内容视图
     */
    protected abstract View createContentView(@NonNull LayoutInflater inflater,
                                              @Nullable ViewGroup container,
                                              @Nullable Bundle savedInstanceState);
}
