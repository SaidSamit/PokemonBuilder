package com.pokemones.pokemonbuilder.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageUtils {
    private static final ExecutorService executor = Executors.newFixedThreadPool(3);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void loadImageIntoImageView(Context ctx, ImageView iv, String url) {
        final WeakReference<ImageView> ref = new WeakReference<>(iv);
        executor.execute(() -> {
            Bitmap b = downloadBitmap(url);
            mainHandler.post(() -> {
                ImageView v = ref.get();
                if (v != null && b != null) v.setImageBitmap(b);
            });
        });
    }

    public static void loadImageIntoButtonFromUrl(Context ctx, ImageButton ib, String url) {
        final WeakReference<ImageButton> ref = new WeakReference<>(ib);
        executor.execute(() -> {
            Bitmap b = downloadBitmap(url);
            mainHandler.post(() -> {
                ImageButton v = ref.get();
                if (v != null && b != null) v.setImageBitmap(b);
            });
        });
    }

    private static Bitmap downloadBitmap(String urlStr) {
        try {
            URL u = new URL(urlStr);
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setDoInput(true);
            c.connect();
            try (InputStream is = c.getInputStream()) {
                return BitmapFactory.decodeStream(is);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap loadBitmapFromPath(String path) {
        try {
            return BitmapFactory.decodeFile(path);
        } catch (Exception e) {
            return null;
        }
    }

    public static String saveBitmapToCache(Context ctx, Bitmap bmp, String name) {
        try {
            File dir = ctx.getExternalCacheDir();
            if (dir == null) dir = ctx.getCacheDir();
            File f = new File(dir, name + ".png");
            try (FileOutputStream out = new FileOutputStream(f)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
                out.flush();
            }
            return f.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
