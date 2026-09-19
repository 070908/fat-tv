package com.fattv.app.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fattv.app.R;
import com.fattv.app.model.Song;
import com.fattv.app.source.PublicSourceAdapter;
import com.fattv.app.source.SourceManager;
import com.fattv.app.source.SourceProvider;
import com.fattv.app.ui.PlayerActivity;
import com.fattv.app.ui.adapter.SongAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * RankFragment - 音乐排行榜页面
 * <p>接入网易云飙升榜真实数据（PublicSourceAdapter.getTopSongs，
 * real-music.js 内部使用 /playlist/track/all?id=19723756 飙升榜），
 * 后台线程加载，SongAdapter 单列列表展示，DPad 支持。点击歌曲跳转播放页。</p>
 */
public class RankFragment extends TranslucentFragment {

    private RecyclerView rvList;
    private TextView tvEmpty;
    private final List<Song> rankSongs = new ArrayList<>();
    private SongAdapter adapter;

    @Override
    protected View createContentView(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_page, container, false);

        TextView tvTitle = view.findViewById(R.id.tv_page_title);
        tvTitle.setText(R.string.page_rank);

        rvList = view.findViewById(R.id.rv_list);
        tvEmpty = view.findViewById(R.id.tv_empty_hint);

        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SongAdapter(rankSongs, this::onSongClick);
        rvList.setAdapter(adapter);

        loadRank();
        return view;
    }

    private void loadRank() {
        tvEmpty.setText(R.string.loading);
        tvEmpty.setVisibility(View.VISIBLE);
        rvList.setVisibility(View.GONE);

        new Thread(() -> {
            List<Song> result = null;
            try {
                SourceProvider provider = SourceManager.getInstance().getProviderByType(SourceProvider.SourceType.PUBLIC);
                if (provider instanceof PublicSourceAdapter) {
                    result = ((PublicSourceAdapter) provider).getTopSongs(20);
                }
            } catch (Exception ignored) {
            }
            final List<Song> songs = result;
            requireActivity().runOnUiThread(() -> {
                rankSongs.clear();
                if (songs != null) rankSongs.addAll(songs);
                adapter.notifyDataSetChanged();

                boolean empty = rankSongs.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvList.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (empty) tvEmpty.setText(R.string.empty_rank);
            });
        }).start();
    }

    private void onSongClick(Song song) {
        Intent intent = new Intent(requireContext(), PlayerActivity.class);
        intent.putExtra("song", song);
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