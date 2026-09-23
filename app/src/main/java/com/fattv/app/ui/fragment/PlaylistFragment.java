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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fattv.app.R;
import com.fattv.app.model.Song;
import com.fattv.app.source.PublicSourceAdapter;
import com.fattv.app.source.SourceManager;
import com.fattv.app.source.SourceProvider;
import com.fattv.app.ui.PlayerActivity;
import com.fattv.app.ui.adapter.CardAdapter;
import com.fattv.app.ui.adapter.SongAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * PlaylistFragment - 精选歌单页面
 * <p>两级状态：</p>
 * <ul>
 *   <li>网格态：CardAdapter + GridLayoutManager 展示精选歌单推荐（getTopPlaylists）</li>
 *   <li>曲目态：点击歌单卡片后切换为 SongAdapter 展示歌单内歌曲（getPlaylistTracks），
 *       标题栏出现"返回歌单"按钮返回网格态</li>
 * </ul>
 * 数据来自 网易云公开音源（real-music.js），后台线程加载，界面支持 DPad 焦点导航。
 */
public class PlaylistFragment extends TranslucentFragment {

    private static final int GRID_COLUMNS = 4;

    private RecyclerView rvList;
    private TextView tvEmpty;
    private TextView tvTitle;
    private TextView btnBack;

    private final List<Song> playlists = new ArrayList<>();
    private final List<Song> tracks = new ArrayList<>();
    private CardAdapter gridAdapter;
    private SongAdapter trackAdapter;

    private boolean inTracksMode = false; // false=歌单网格态, true=歌单曲目态

    @Override
    protected View createContentView(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album, container, false);

        tvTitle = view.findViewById(R.id.tv_page_title);
        tvEmpty = view.findViewById(R.id.tv_empty_hint);
        btnBack = view.findViewById(R.id.btn_back_album);
        rvList = view.findViewById(R.id.rv_list);

        btnBack.setText(R.string.back_to_playlist);

        // 歌单网格：4 列
        gridAdapter = new CardAdapter(playlists, this::onPlaylistClick);
        rvList.setLayoutManager(new GridLayoutManager(requireContext(), GRID_COLUMNS));

        // 曲目列表：单列
        trackAdapter = new SongAdapter(tracks, this::onTrackClick);

        btnBack.setOnClickListener(v -> {
            switchToGridMode();
            // 返回按钮 GONE 后焦点会丢失回 tab_home，接住焦点落到网格第一项
            rvList.post(() -> {
                if (!inTracksMode) focusListFirst(rvList);
            });
        });

        switchToGridMode();
        loadPlaylists();
        return view;
    }

    private void switchToGridMode() {
        inTracksMode = false;
        btnBack.setVisibility(View.GONE);
        tvTitle.setText(R.string.page_playlist);
        rvList.setLayoutManager(new GridLayoutManager(requireContext(), GRID_COLUMNS));
        rvList.getRecycledViewPool().clear(); // 防止网格态/曲目态切换时复用错 Adapter 的 ViewHolder
        rvList.swapAdapter(gridAdapter, true);
    }

    private void switchToTracksMode(Song playlist) {
        inTracksMode = true;
        btnBack.setVisibility(View.VISIBLE);
        tvTitle.setText(playlist.title);
        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.getRecycledViewPool().clear(); // 防止网格态/曲目态切换时复用错 Adapter 的 ViewHolder
        rvList.swapAdapter(trackAdapter, true);

        // 卡片点击后 rvList 即将 GONE，焦点会丢失并被系统回退到 tab_home；
        // 主动把焦点接住到「返回歌单」按钮，保证 DPad 导航不中断
        btnBack.post(() -> {
            if (inTracksMode) btnBack.requestFocus();
        });

        tvEmpty.setText(R.string.loading);
        tvEmpty.setVisibility(View.VISIBLE);
        rvList.setVisibility(View.GONE);

        new Thread(() -> {
            List<Song> result = loadPlaylistTracks(playlist.id);
            requireActivity().runOnUiThread(() -> {
                tracks.clear();
                if (result != null) tracks.addAll(result);
                trackAdapter.notifyDataSetChanged();

                boolean empty = tracks.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvList.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (empty) tvEmpty.setText(R.string.empty_playlist_tracks);
            });
        }).start();
    }

    private void loadPlaylists() {
        tvEmpty.setText(R.string.loading);
        tvEmpty.setVisibility(View.VISIBLE);
        rvList.setVisibility(View.GONE);

        new Thread(() -> {
            List<Song> result = loadTopPlaylists();
            requireActivity().runOnUiThread(() -> {
                playlists.clear();
                if (result != null) playlists.addAll(result);
                gridAdapter.notifyDataSetChanged();

                boolean empty = playlists.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvList.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (empty) tvEmpty.setText(R.string.empty_playlist);
            });
        }).start();
    }

    private List<Song> loadTopPlaylists() {
        try {
            SourceProvider provider = SourceManager.getInstance().getProviderByType(SourceProvider.SourceType.PUBLIC);
            if (provider instanceof PublicSourceAdapter) {
                return ((PublicSourceAdapter) provider).getTopPlaylists(12);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private List<Song> loadPlaylistTracks(String playlistId) {
        try {
            SourceProvider provider = SourceManager.getInstance().getProviderByType(SourceProvider.SourceType.PUBLIC);
            if (provider instanceof PublicSourceAdapter) {
                return ((PublicSourceAdapter) provider).getPlaylistTracks(playlistId);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 点击歌单卡片：切换为歌单曲目态
     */
    private void onPlaylistClick(Song playlist) {
        if (playlist == null || playlist.id == null) return;
        switchToTracksMode(playlist);
    }

    /**
     * 点击曲目：跳转播放页
     */
    private void onTrackClick(Song song) {
        Intent intent = new Intent(requireContext(), PlayerActivity.class);
        intent.putExtra("song", song);
        intent.putExtra("queue", new java.util.ArrayList<>(tracks));
        startActivity(intent);
    }

    @Override
    public boolean requestContentFocus() {
        if (contentView == null) return false;
        // 曲目态：先聚焦「返回歌单」按钮，保证 DPad 可退出到网格态
        if (inTracksMode && btnBack.getVisibility() == View.VISIBLE) {
            btnBack.requestFocus();
            return true;
        }
        if (rvList != null) {
            focusListFirst(rvList);
            return true;
        }
        return super.requestContentFocus();
    }
}