package com.fattv.app.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.fattv.app.R;
import com.fattv.app.model.Song;
import com.fattv.app.source.SourceManager;
import com.fattv.app.ui.PlayerActivity;

/**
 * MusicPlaybackService - Media3 ExoPlayer + MediaSession 后台播放服务
 */
public class MusicPlaybackService extends MediaSessionService {
    private static final String CHANNEL_ID = "fat_tv_playback";
    private static final int NOTIF_ID = 1;

    private ExoPlayer player;
    private MediaSession mediaSession;
    private NotificationCompat.Builder notificationBuilder;

    public static ExoPlayer getPlayerInstance() {
        return Holder.INSTANCE != null ? Holder.INSTANCE.player : null;
    }

    public static MusicPlaybackService getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        player = new ExoPlayer.Builder(this).build();
        player.setRepeatMode(Player.REPEAT_MODE_ALL);

        mediaSession = new MediaSession.Builder(this, player).build();
        startForeground(NOTIF_ID, buildNotification());

        Holder.INSTANCE = this;
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    public void playSong(Song song) {
        new Thread(() -> {
            try {
                String url = SourceManager.getInstance().getCurrentProvider().resolveUrl(song);
                new Handler(Looper.getMainLooper()).post(() -> {
                    player.setMediaItem(MediaItem.fromUri(url));
                    player.prepare();
                    player.play();
                    updateNotification(song.title + " - " + song.artist);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "fat TV \u64ad\u653e", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
            new Intent(this, PlayerActivity.class), PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("fat TV")
            .setContentText("\u6b63\u5728\u64ad\u653e\u97f3\u4e50...")
            .setSmallIcon(R.drawable.ic_logo)
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("fat TV")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_logo)
            .setOngoing(true)
            .build());
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) mediaSession.release();
        if (player != null) player.release();
        Holder.INSTANCE = null;
        super.onDestroy();
    }

    private static class Holder {
        static MusicPlaybackService INSTANCE;
    }
}
