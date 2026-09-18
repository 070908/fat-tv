package com.fattv.app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fattv.app.R;

/**
 * MvFragment - MV 页面（占位）：未来接入 MV 搜索与播放
 */
public class MvFragment extends TranslucentFragment {

    @Override
    protected View createContentView(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);

        TextView tvTitle = view.findViewById(R.id.tv_page_title);
        TextView tvHint = view.findViewById(R.id.tv_hint);
        tvTitle.setText(R.string.page_mv);
        tvHint.setText(R.string.coming_soon);

        return view;
    }
}
