package com.fattv.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fattv.app.R;
import com.fattv.app.model.Song;
import com.fattv.app.player.MusicPlaybackService;
import com.fattv.app.ui.adapter.SongAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * PlaylistActivity - TV 播放列表页：展示最近播放历史，点击可再次播放
 */
public class PlaylistActivity extends AppCompatActivity {
    private RecyclerView rvPlaylist;
    private TextView tvEmptyHint;
    private SongAdapter adapter;
    private final List<Song> playlist = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        rvPlaylist = findViewById(R.id.rv_playlist);
        tvEmptyHint = findViewById(R.id.tv_empty_hint);
        rvPlaylist.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(playlist, this::onSongClick);
        rvPlaylist.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHistory();
    }

    private void refreshHistory() {
        playlist.clear();
        List<Song> history = MusicPlaybackService.getPlayHistory();
        if (history != null) {
            playlist.addAll(history);
        }
        adapter.notifyDataSetChanged();

        boolean empty = playlist.isEmpty();
        tvEmptyHint.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvPlaylist.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) {
            rvPlaylist.post(() -> {
                RecyclerView.ViewHolder holder = rvPlaylist.findViewHolderForAdapterPosition(0);
                if (holder != null) holder.itemView.requestFocus();
            });
        }
    }

    private void onSongClick(Song song) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("song", song);
        startActivity(intent);
    }
}