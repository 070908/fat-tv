package com.fattv.app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fattv.app.R;

/**
 * AlbumFragment - 专辑页面（占位）：未来接入专辑推荐/专辑列表
 */
public class AlbumFragment extends TranslucentFragment {

    @Override
    protected View createContentView(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);

        TextView tvTitle = view.findViewById(R.id.tv_page_title);
        TextView tvHint = view.findViewById(R.id.tv_hint);
        tvTitle.setText(R.string.page_album);
        tvHint.setText(R.string.coming_soon);

        return view;
    }
}
