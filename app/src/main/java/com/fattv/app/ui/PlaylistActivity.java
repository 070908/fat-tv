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
import com.fattv.app.source.PublicSourceAdapter;
import com.fattv.app.source.SourceManager;
import com.fattv.app.source.SourceProvider;
import com.fattv.app.ui.adapter.SongAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * PlaylistActivity - TV 播放列表页
 * <p>两种模式：
 * 1. 带 playlist_id/album_id 参数时：加载歌单/专辑曲目列表（供发现页/首页歌单卡使用）
 * 2. 无参数时：展示最近播放历史，点击可再次播放（原"播放列表"入口）</p>
 */
public class PlaylistActivity extends AppCompatActivity {
    private static final String EXTRA_PLAYLIST_ID = "playlist_id";
    private static final String EXTRA_PLAYLIST_NAME = "playlist_name";
    private static final String EXTRA_ALBUM_ID = "album_id";
    private static final String EXTRA_ALBUM_NAME = "album_name";

    private RecyclerView rvPlaylist;
    private TextView tvEmptyHint;
    private TextView tvTitle;
    private SongAdapter adapter;
    private final List<Song> playlist = new ArrayList<>();

    private String playlistId;
    private String albumId;
    private String albumName;
    private boolean isDetailMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        playlistId = getIntent().getStringExtra(EXTRA_PLAYLIST_ID);
        albumId = getIntent().getStringExtra(EXTRA_ALBUM_ID);
        albumName = getIntent().getStringExtra(EXTRA_ALBUM_NAME);
        String playlistName = getIntent().getStringExtra(EXTRA_PLAYLIST_NAME);
        isDetailMode = playlistId != null || albumId != null;

        rvPlaylist = findViewById(R.id.rv_playlist);
        tvEmptyHint = findViewById(R.id.tv_empty_hint);
        tvTitle = findViewById(R.id.tv_playlist_title);
        rvPlaylist.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(playlist, this::onSongClick);
        rvPlaylist.setAdapter(adapter);

        if (isDetailMode) {
            String title = playlistName != null ? playlistName : albumName;
            tvTitle.setText(title != null ? title : getString(R.string.playlist));
            loadDetailTracks();
        } else {
            tvTitle.setText(R.string.playlist);
            refreshHistory();
        }
    }

    /** 加载歌单/专辑曲目 */
    private void loadDetailTracks() {
        tvEmptyHint.setText(R.string.loading);
        tvEmptyHint.setVisibility(View.VISIBLE);
        rvPlaylist.setVisibility(View.GONE);

        new Thread(() -> {
            List<Song> result = null;
            try {
                SourceProvider provider = SourceManager.getInstance()
                        .getProviderByType(SourceProvider.SourceType.PUBLIC);
                if (provider instanceof PublicSourceAdapter) {
                    PublicSourceAdapter pub = (PublicSourceAdapter) provider;
                    if (albumId != null) {
                        result = pub.getAlbumTracks(albumId, albumName != null ? albumName : "");
                    } else {
                        result = pub.getPlaylistTracks(playlistId);
                    }
                }
            } catch (Exception ignored) {
            }
            final List<Song> songs = result;
            runOnUiThread(() -> {
                playlist.clear();
                if (songs != null) playlist.addAll(songs);
                adapter.notifyDataSetChanged();

                boolean empty = playlist.isEmpty();
                tvEmptyHint.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvPlaylist.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (empty) {
                    tvEmptyHint.setText(R.string.playlist_load_failed);
                } else {
                    rvPlaylist.post(() -> {
                        RecyclerView.ViewHolder holder =
                                rvPlaylist.findViewHolderForAdapterPosition(0);
                        if (holder != null) holder.itemView.requestFocus();
                    });
                }
            });
        }).start();
    }

    /** 播放历史模式 */
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
        if (empty) {
            tvEmptyHint.setText(R.string.playlist_empty_history);
        } else {
            rvPlaylist.post(() -> {
                RecyclerView.ViewHolder holder = rvPlaylist.findViewHolderForAdapterPosition(0);
                if (holder != null) holder.itemView.requestFocus();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isDetailMode) {
            refreshHistory();
        }
    }

    private void onSongClick(Song song) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("song", song);
        intent.putExtra("queue", new java.util.ArrayList<>(playlist));
        startActivity(intent);
    }
}