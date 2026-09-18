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
import com.fattv.app.player.MusicPlaybackService;
import com.fattv.app.ui.PlayerActivity;
import com.fattv.app.ui.adapter.SongAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * PlaylistFragment - 歌单页面：展示用户播放历史/歌单
 */
public class PlaylistFragment extends TranslucentFragment {

    private RecyclerView rvPlaylist;
    private TextView tvEmpty;
    private SongAdapter adapter;
    private final List<Song> playlist = new ArrayList<>();

    @Override
    protected View createContentView(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_page, container, false);

        TextView tvTitle = view.findViewById(R.id.tv_page_title);
        tvTitle.setText(R.string.page_playlist);

        rvPlaylist = view.findViewById(R.id.rv_list);
        tvEmpty = view.findViewById(R.id.tv_empty_hint);

        rvPlaylist.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SongAdapter(playlist, this::onSongClick);
        rvPlaylist.setAdapter(adapter);

        refreshPlaylist();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPlaylist();
    }

    private void refreshPlaylist() {
        playlist.clear();
        List<Song> history = MusicPlaybackService.getPlayHistory();
        if (history != null) {
            playlist.addAll(history);
        }
        adapter.notifyDataSetChanged();

        boolean empty = playlist.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvPlaylist.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) {
            rvPlaylist.post(() -> {
                RecyclerView.ViewHolder holder = rvPlaylist.findViewHolderForAdapterPosition(0);
                if (holder != null) holder.itemView.requestFocus();
            });
        }
    }

    private void onSongClick(Song song) {
        Intent intent = new Intent(requireContext(), PlayerActivity.class);
        intent.putExtra("song", song);
        startActivity(intent);
    }
}
