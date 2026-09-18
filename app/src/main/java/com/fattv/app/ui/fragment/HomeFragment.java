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
import com.fattv.app.player.MusicPlaybackService;
import com.fattv.app.ui.PlayerActivity;
import com.fattv.app.ui.adapter.SongAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * HomeFragment - 首页：每日推荐卡片 + 最近播放
 */
public class HomeFragment extends TranslucentFragment {

    private RecyclerView rvRecent;
    private TextView tvEmpty;
    private SongAdapter adapter;
    private final List<Song> recentSongs = new ArrayList<>();

    @Override
    protected View createContentView(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvRecent = view.findViewById(R.id.rv_recent);
        tvEmpty = view.findViewById(R.id.tv_empty_hint);

        // 最近播放横向滚动（2 列网格，类似参考图的卡片布局）
        rvRecent.setLayoutManager(new GridLayoutManager(requireContext(), 2,
                RecyclerView.HORIZONTAL, false));
        adapter = new SongAdapter(recentSongs, this::onSongClick);
        rvRecent.setAdapter(adapter);

        // 各入口按钮
        View btnSearch = view.findViewById(R.id.btn_quick_search);
        View btnPlaylist = view.findViewById(R.id.btn_quick_playlist);
        View btnSettings = view.findViewById(R.id.btn_quick_settings);

        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                // 通知 MainActivity 切换到搜索 Tab
                if (getActivity() instanceof com.fattv.app.ui.MainActivity) {
                    ((com.fattv.app.ui.MainActivity) getActivity()).switchToTab(1);
                }
            });
        }
        if (btnPlaylist != null) {
            btnPlaylist.setOnClickListener(v -> {
                if (getActivity() instanceof com.fattv.app.ui.MainActivity) {
                    ((com.fattv.app.ui.MainActivity) getActivity()).switchToTab(1);
                }
            });
        }
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.fattv.app.ui.SettingsActivity.class);
                startActivity(intent);
            });
        }

        setupFocus(btnSearch);
        setupFocus(btnPlaylist);
        setupFocus(btnSettings);

        refreshRecent();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshRecent();
    }

    private void refreshRecent() {
        recentSongs.clear();
        List<Song> history = MusicPlaybackService.getPlayHistory();
        if (history != null) {
            // 最多取前 10 首
            int limit = Math.min(10, history.size());
            for (int i = 0; i < limit; i++) {
                recentSongs.add(history.get(i));
            }
        }
        adapter.notifyDataSetChanged();
        boolean empty = recentSongs.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvRecent.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void onSongClick(Song song) {
        Intent intent = new Intent(requireContext(), PlayerActivity.class);
        intent.putExtra("song", song);
        startActivity(intent);
    }

    private void setupFocus(View view) {
        if (view == null) return;
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.1f : 1.0f;
            v.animate().scaleX(scale).scaleY(scale).setDuration(200).start();
        });
    }
}
