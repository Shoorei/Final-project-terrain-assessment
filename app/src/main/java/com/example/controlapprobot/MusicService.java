package com.example.controlapprobot;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;
    private List<Integer> playlist = new ArrayList<>();
    private int currentTrackIndex = 0;

    private final float MAX_VOLUME = 0.05f; // target volume
    private final int FADE_DURATION = 1000; // 1 second
    private final int FADE_INTERVAL = 100; // volume step every 100ms

    @Override
    public void onCreate() {
        super.onCreate();

//        playlist.add(R.raw.many_times_with_you);
//        playlist.add(R.raw.atarashii_hashiri_wo);
        playlist.add(R.raw.peaceful_sleep);
        playlist.add(R.raw.city_ruins);
        playlist.add(R.raw.amusement_park);
        playlist.add(R.raw.vague_hope);

        Collections.shuffle(playlist);

        playNextTrack();
    }

    private void playNextTrack() {
        if (mediaPlayer != null) {
            fadeOutAndRelease(() -> {
                currentTrackIndex = (currentTrackIndex + 1) % playlist.size();
                startNewTrackWithFadeIn();
            });
        } else {
            startNewTrackWithFadeIn();
        }
    }

    private void startNewTrackWithFadeIn() {
        mediaPlayer = MediaPlayer.create(this, playlist.get(currentTrackIndex));
        mediaPlayer.setVolume(0f, 0f); // start silent
        mediaPlayer.setOnCompletionListener(mp -> playNextTrack());
        mediaPlayer.start();
        fadeIn(mediaPlayer);
    }

    private void fadeIn(MediaPlayer player) {
        final Handler handler = new Handler();
        final float deltaVolume = MAX_VOLUME / (FADE_DURATION / FADE_INTERVAL);

        Runnable fadeInRunnable = new Runnable() {
            float volume = 0f;

            @Override
            public void run() {
                if (player != null && volume < MAX_VOLUME) {
                    volume += deltaVolume;
                    player.setVolume(volume, volume);
                    handler.postDelayed(this, FADE_INTERVAL);
                } else {
                    player.setVolume(MAX_VOLUME, MAX_VOLUME);
                }
            }
        };
        handler.post(fadeInRunnable);
    }

    private void fadeOutAndRelease(Runnable onComplete) {
        final Handler handler = new Handler();
        final float deltaVolume = MAX_VOLUME / (FADE_DURATION / FADE_INTERVAL);

        Runnable fadeOutRunnable = new Runnable() {
            float volume = MAX_VOLUME;

            @Override
            public void run() {
                if (mediaPlayer != null && volume > 0f) {
                    volume -= deltaVolume;
                    mediaPlayer.setVolume(Math.max(0f, volume), Math.max(0f, volume));
                    handler.postDelayed(this, FADE_INTERVAL);
                } else {
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                    onComplete.run();
                }
            }
        };
        handler.post(fadeOutRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
