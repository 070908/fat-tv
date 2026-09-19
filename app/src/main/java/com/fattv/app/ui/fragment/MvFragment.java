package com.fattv.app.ui.fragment;

import android.content.Intent;
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
import com.fattv.app.model.Song;
import com.fattv.app.source.PublicSourceAdapter;
import com.fattv.app.source.SourceManager;
import com.fattv.app.source.SourceProvider;
import com.fattv.app.ui.MvPlayerActivity;
import com.fattv.app.ui.adapter.CardAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * MvFragment - 高清 MV 页面
 * <p>接入网易云 MV 真实数据（PublicSourceAdapter.getTopMvs），
 * CardAdapter 网格展示，点击跳转 MvPlayerActivity 播放。DPad 支持。</p>
 */
public class MvFragment extends TranslucentFragment {

    private static final int GRID_COLUMNS = 4;

    private RecyclerView rvList;
    private TextView tvEmpty;
    private final List<Song> mvList = new ArrayList<>();
    private CardAdapter adapter;

    @Override
    protected View createContentView(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_page, container, false);

        TextView tvTitle = view.findViewById(R.id.tv_page_title);
        tvTitle.setText(R.string.page_mv);

        rvList = view.findViewById(R.id.rv_list);
        tvEmpty = view.findViewById(R.id.tv_empty_hint);

        rvList.setLayoutManager(new GridLayoutManager(requireContext(), GRID_COLUMNS));
        adapter = new CardAdapter(mvList, this::onMvClick);
        rvList.setAdapter(adapter);

        loadMvs();
        return view;
    }

    private void loadMvs() {
        tvEmpty.setText(R.string.loading);
        tvEmpty.setVisibility(View.VISIBLE);
        rvList.setVisibility(View.GONE);

        new Thread(() -> {
            List<Song> result = null;
            try {
                SourceProvider provider = SourceManager.getInstance().getProviderByType(SourceProvider.SourceType.PUBLIC);
                if (provider instanceof PublicSourceAdapter) {
                    result = ((PublicSourceAdapter) provider).getTopMvs(12);
                }
            } catch (Exception ignored) {
            }
            final List<Song> songs = result;
            requireActivity().runOnUiThread(() -> {
                mvList.clear();
                if (songs != null) mvList.addAll(songs);
                adapter.notifyDataSetChanged();

                boolean empty = mvList.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvList.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (empty) tvEmpty.setText(R.string.empty_mv);
            });
        }).start();
    }

    private void onMvClick(Song mv) {
        if (mv == null || mv.id == null) return;
        Intent intent = new Intent(requireContext(), MvPlayerActivity.class);
        intent.putExtra("song", mv);
        startActivity(intent);
    }

    @Override
    public boolean requestContentFocus() {
        if (contentView == null) return false;
        if (rvList != null) {
            focusListFirst(rvList);
            return true;
        }
        return super.requestContentFocus();
    }
}