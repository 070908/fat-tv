package com.fattv.app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.fattv.app.R;

/**
 * TranslucentFragment - TV 半透明 Fragment 基类
 * 所有 Tab 页面继承此类，自动应用全局海绵宝宝背景 + 半透明内容面板。
 * <p>
 * 同时提供统一的焦点落点入口 {@link #requestContentFocus()}：
 * MainActivity 在 Tab 栏收到 DPAD_DOWN 时调用，把焦点落入页面内容区
 * 第一个可聚焦控件/列表项（数据未加载完成时自动重试）。
 */
public abstract class TranslucentFragment extends Fragment {

    /** 子类 createContentView() 返回的内容视图（供焦点导航与子类持有） */
    protected View contentView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 根布局：外层 FrameLayout 承载全局背景
        FrameLayout root = new FrameLayout(requireContext());
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // 全局背景层（海绵宝宝 bg_main，alpha 0.55）
        View bgView = new View(requireContext());
        bgView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        bgView.setBackgroundResource(R.drawable.bg_main);
        bgView.setAlpha(0.55f);
        root.addView(bgView);

        // 极轻暗色遮罩
        View overlay = new View(requireContext());
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(getResources().getColor(R.color.bg_overlay_light));
        root.addView(overlay);

        // 半透明内容面板
        contentView = createContentView(inflater, container, savedInstanceState);
        if (contentView != null) {
            contentView.setBackgroundResource(R.color.bg_surface_light);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            lp.setMargins(
                    getResources().getDimensionPixelSize(R.dimen.tv_safe_margin),
                    getResources().getDimensionPixelSize(R.dimen.tv_safe_margin) + 120,
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

    /**
     * 统一焦点入口：将焦点交给内容区第一个可聚焦控件。
     * 默认递归查找；子类可覆写为更精准的列表落点逻辑。
     */
    public boolean requestContentFocus() {
        if (contentView == null) return false;
        View target = findFirstFocusable(contentView);
        if (target != null) {
            target.requestFocus();
            return true;
        }
        // 兜底：聚焦页面标题（标题默认可聚焦）
        TextView title = contentView.findViewById(R.id.tv_page_title);
        if (title != null) {
            title.setFocusable(true);
            title.requestFocus();
            return true;
        }
        return false;
    }

    /**
     * 递归查找内容区第一个可聚焦元素：
     * <ul>
     *   <li>RecyclerView：聚焦列表第一项（数据未就绪时 post + 重试）</li>
     *   <li>可聚焦控件：直接返回</li>
     * </ul>
     */
    private View findFirstFocusable(View view) {
        if (view == null) return null;
        if (view instanceof RecyclerView) {
            RecyclerView rv = (RecyclerView) view;
            boolean hasData = rv.getAdapter() != null
                    && rv.getAdapter().getItemCount() > 0
                    && rv.getVisibility() == View.VISIBLE;
            if (hasData) {
                focusListFirst(rv);
                return view;
            }
            // 空列表或不可见：跳过，继续向下查找其他可聚焦控件
        } else if (view.isFocusable() && view.isShown()) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View result = findFirstFocusable(vg.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }

    /**
     * 聚焦 RecyclerView 第一个可见项；数据未加载完成时以 120ms 间隔自动重试（最多 10 次）。
     */
    protected void focusListFirst(final RecyclerView rv) {
        if (rv == null) return;
        rv.scrollToPosition(0);
        rv.post(new Runnable() {
            int attempts = 0;

            @Override
            public void run() {
                if (rv.getChildCount() > 0) {
                    View first = rv.getChildAt(0);
                    if (first != null && first.isFocusable()) {
                        first.requestFocus();
                        return;
                    }
                }
                if (attempts++ < 10) {
                    rv.postDelayed(this, 120);
                }
            }
        });
    }
}