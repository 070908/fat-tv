package com.fattv.app.ui.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fattv.app.R;
import com.fattv.app.model.Song;
import com.fattv.app.source.LocalMusicScanner;
import com.fattv.app.ui.PlayerActivity;
import com.fattv.app.ui.adapter.SongAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * LocalMusicFragment - 本地音乐页面
 * <p>三层实现：</p>
 * <ol>
 *   <li>权限：API 33+ 申请 READ_MEDIA_AUDIO，API 23-32 申请 READ_EXTERNAL_STORAGE</li>
 *   <li>扫描：后台线程 LocalMusicScanner.scanAll() 查询 MediaStore + 遍历外置U盘 + 常见音乐目录</li>
 *   <li>展示：SongAdapter 列表（歌名/歌手/时长/来源），点击跳转 PlayerActivity 播放</li>
 * </ol>
 */
public class LocalMusicFragment extends TranslucentFragment {

    private static final int REQ_STORAGE = 1001;

    private RecyclerView rvList;
    private TextView tvEmpty;
    private final List<Song> localSongs = new ArrayList<>();
    private SongAdapter adapter;
    private boolean permissionRequested = false;

    @Override
    protected View createContentView(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_page, container, false);

        TextView tvTitle = view.findViewById(R.id.tv_page_title);
        tvTitle.setText(R.string.page_local);

        rvList = view.findViewById(R.id.rv_list);
        tvEmpty = view.findViewById(R.id.tv_empty_hint);

        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SongAdapter(localSongs, this::onSongClick);
        rvList.setAdapter(adapter);

        // 提升空态提示可见度：白色加粗大字号
        tvEmpty.setTextColor(getResources().getColor(android.R.color.white));
        tvEmpty.setTypeface(null, android.graphics.Typeface.BOLD);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!permissionRequested) {
            permissionRequested = true;
            checkPermissionAndScan();
        }
    }

    /**
     * 权限检查 → 自动扫描（三层中的第一层：运行时权限）
     */
    private void checkPermissionAndScan() {
        String permission = requiredPermission();
        if (permission == null) {
            // API < 23：无需运行时权限，直接扫描
            scanAndShow();
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED) {
            scanAndShow();
            return;
        }

        tvEmpty.setText(R.string.need_storage_permission);
        tvEmpty.setVisibility(View.VISIBLE);
        rvList.setVisibility(View.GONE);
        requestPermissions(new String[]{permission}, REQ_STORAGE);
    }

    private String requiredPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            return Manifest.permission.READ_MEDIA_AUDIO;
        } else if (Build.VERSION.SDK_INT >= 23) {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanAndShow();
            } else {
                tvEmpty.setText(R.string.permission_denied);
                tvEmpty.setVisibility(View.VISIBLE);
                rvList.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 后台线程扫描本地音乐并展示（三层中的第二/三层：扫描 + UI 展示）
     */
    private void scanAndShow() {
        tvEmpty.setText(R.string.scanning);
        tvEmpty.setVisibility(View.VISIBLE);
        rvList.setVisibility(View.GONE);

        new Thread(() -> {
            android.content.Context ctx = requireContext();
            List<Song> result = LocalMusicScanner.scanAll(ctx);
            requireActivity().runOnUiThread(() -> {
                localSongs.clear();
                localSongs.addAll(result);
                adapter.notifyDataSetChanged();

                boolean empty = localSongs.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvList.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (empty) tvEmpty.setText(R.string.empty_local);
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