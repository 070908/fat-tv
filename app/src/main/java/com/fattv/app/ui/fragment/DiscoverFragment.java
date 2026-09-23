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
import com.fattv.app.ui.PlaylistActivity;
import com.fattv.app.ui.PlayerActivity;
import com.fattv.app.ui.adapter.CardAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * DiscoverFragment - 发现页：左侧子导航 + 右侧内容网格
 * <p>合并原"歌单"和"album"功能，扩展为4类内容发现：
 * 精选歌单 / 新碟上架 / 热门歌手 / 风格分类。</p>
 */
public class DiscoverFragment extends TranslucentFragment {

    private static final int TYPE_PLAYLIST = 0;
    private static final int TYPE_ALBUM = 1;
    private static final int TYPE_ARTIST = 2;
    private static final int TYPE_GENRE = 3;

    private int currentType = TYPE_PLAYLIST;

    private TextView[] navItems;
    private int[] navIds = {R.id.nav_playlist, R.id.nav_album, R.id.nav_artist, R.id.nav_genre};
    private String[] navLabels = {"精选歌单", "新碟上架", "热门歌手", "风格分类"};

    private RecyclerView rvContent;
    private TextView tvEmpty;
    private CardAdapter contentAdapter;
    private final List<Song> contentItems = new ArrayList<>();

    @Override
    protected View createContentView(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        // 绑定左侧导航
        navItems = new TextView[navIds.length];
        for (int i = 0; i < navIds.length; i++) {
            navItems[i] = view.findViewById(navIds[i]);
            final int index = i;
            navItems[i].setOnClickListener(v -> switchType(index));
            navItems[i].setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && index != currentType) {
                    // 焦点在导航项上时高亮但不自动切换，等用户按确认
                    navItems[index].setTextColor(getResources().getColor(R.color.text_accent));
                } else {
                    updateNavVisuals();
                }
            });
        }

        // 绑定右侧内容区
        rvContent = view.findViewById(R.id.rv_discover_content);
        tvEmpty = view.findViewById(R.id.tv_discover_empty);

        // 3列网格（TV端横向较宽，3列合适）
        rvContent.setLayoutManager(new GridLayoutManager(requireContext(), 3,
                RecyclerView.VERTICAL, false));
        contentAdapter = new CardAdapter(contentItems, this::onItemClick);
        rvContent.setAdapter(contentAdapter);

        // 默认加载歌单
        switchType(TYPE_PLAYLIST);
        return view;
    }

    private void switchType(int type) {
        if (type < 0 || type >= navIds.length) return;
        currentType = type;
        updateNavVisuals();
        loadContent(type);
    }

    private void updateNavVisuals() {
        for (int i = 0; i < navItems.length; i++) {
            boolean selected = (i == currentType);
            navItems[i].setTextColor(getResources().getColor(
                    selected ? R.color.text_accent : R.color.text_secondary));
        }
    }

    private void loadContent(int type) {
        tvEmpty.setText(R.string.loading);
        tvEmpty.setVisibility(View.VISIBLE);
        rvContent.setVisibility(View.GONE);
        contentItems.clear();
        contentAdapter.notifyDataSetChanged();

        new Thread(() -> {
            List<Song> result = null;
            try {
                SourceProvider provider = SourceManager.getInstance()
                        .getProviderByType(SourceProvider.SourceType.PUBLIC);
                if (provider instanceof PublicSourceAdapter) {
                    PublicSourceAdapter pub = (PublicSourceAdapter) provider;
                    switch (type) {
                        case TYPE_PLAYLIST:
                            result = pub.getTopPlaylists(18);
                            break;
                        case TYPE_ALBUM:
                            result = pub.getTopAlbums(18);
                            break;
                        case TYPE_ARTIST:
                            // 暂用歌单数据作为占位，后续实现歌手专用API
                            result = pub.getTopPlaylists(18);
                            break;
                        case TYPE_GENRE:
                            // 暂用专辑数据作为占位，后续实现风格分类API
                            result = pub.getTopAlbums(18);
                            break;
                    }
                }
            } catch (Exception ignored) {
            }
            final List<Song> songs = result;
            requireActivity().runOnUiThread(() -> {
                contentItems.clear();
                if (songs != null) contentItems.addAll(songs);
                contentAdapter.notifyDataSetChanged();

                boolean empty = contentItems.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvContent.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (empty) {
                    tvEmpty.setText(type == TYPE_ARTIST || type == TYPE_GENRE
                            ? "该分类功能开发中，敬请期待"
                            : navLabels[type] + "加载失败，请检查网络");
                }
            });
        }).start();
    }

    private void onItemClick(Song item) {
        switch (currentType) {
            case TYPE_PLAYLIST:
            case TYPE_ARTIST:
                // 歌单数据（热门歌手暂用歌单占位）：进入歌单曲目列表
                Intent plIntent = new Intent(requireContext(), PlaylistActivity.class);
                plIntent.putExtra("playlist_id", item.id);
                plIntent.putExtra("playlist_name", item.title);
                startActivity(plIntent);
                break;
            case TYPE_ALBUM:
            case TYPE_GENRE:
                // 专辑数据（风格分类暂用专辑占位）：按专辑加载曲目
                Intent alIntent = new Intent(requireContext(), PlaylistActivity.class);
                alIntent.putExtra("album_id", item.id);
                alIntent.putExtra("album_name", item.title);
                startActivity(alIntent);
                break;
        }
    }
}
