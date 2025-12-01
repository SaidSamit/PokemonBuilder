package com.pokemones.pokemonbuilder.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class AudioUtils {
    private static final String TAG = "AudioUtils";
    private static MediaRecorder recorder;
    private static MediaPlayer player;
    private static String lastPath;

    public static boolean isRecording() {
        return recorder != null;
    }

    public static String startRecording(Context ctx, String name) {
        try {
            if (recorder != null) {
                Log.w(TAG, "Recorder already active");
                return lastPath;
            }

            File dir = ctx.getExternalCacheDir();
            if (dir == null) dir = ctx.getCacheDir();
            File f = new File(dir, name + ".3gp");
            lastPath = f.getAbsolutePath();

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(lastPath);

            try {
                recorder.prepare();
            } catch (IOException prepareEx) {
                Log.e(TAG, "prepare() failed: " + prepareEx.getMessage(), prepareEx);
                recorder.release();
                recorder = null;
                lastPath = null;
                return null;
            }

            try {
                recorder.start();
            } catch (RuntimeException startEx) {
                Log.e(TAG, "start() failed: " + startEx.getMessage(), startEx);
                try { recorder.release(); } catch (Exception ignored) {}
                recorder = null;
                lastPath = null;
                return null;
            }

            Log.i(TAG, "Recording started: " + lastPath);
            return lastPath;
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException startRecording: " + se.getMessage(), se);
            if (recorder != null) {
                try { recorder.release(); } catch (Exception ignored) {}
                recorder = null;
            }
            lastPath = null;
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error startRecording: " + e.getMessage(), e);
            if (recorder != null) {
                try { recorder.release(); } catch (Exception ignored) {}
                recorder = null;
            }
            lastPath = null;
            return null;
        }
    }

    public static boolean stopRecording() {
        if (recorder == null) return false;
        try {
            recorder.stop();
        } catch (RuntimeException stopEx) {
            Log.e(TAG, "stop() failed: " + stopEx.getMessage(), stopEx);
        } finally {
            try { recorder.release(); } catch (Exception ignored) {}
            recorder = null;
        }
        Log.i(TAG, "Recording stopped, file: " + lastPath);
        return true;
    }

    public static void play(Context ctx, String path) {
        try {
            if (path == null) return;
            stopPlay();
            player = new MediaPlayer();
            player.setDataSource(path);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(TAG, "play failed: " + e.getMessage(), e);
        } catch (IllegalArgumentException ia) {
            Log.e(TAG, "play illegal arg: " + ia.getMessage(), ia);
        }
    }

    public static void stopPlay() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
                player = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "stopPlay error: " + e.getMessage(), e);
            player = null;
        }
    }

    public static String getLastPath() { return lastPath; }
}
