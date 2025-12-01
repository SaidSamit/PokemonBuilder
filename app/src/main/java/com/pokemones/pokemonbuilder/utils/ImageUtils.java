package com.pokemones.pokemonbuilder.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageUtils {
    public static void loadImageIntoImageView(Context ctx, ImageView iv, String url) {
        new ImageLoadTask(iv).execute(url);
    }

    public static void loadImageIntoButtonFromUrl(Context ctx, ImageButton ib, String url) {
        new ImageLoadButtonTask(ib).execute(url);
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
            File f = new File(ctx.getExternalCacheDir(), name + ".png");
            FileOutputStream out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
            return f.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private static class ImageLoadTask extends AsyncTask<String, Void, Bitmap> {
        private final ImageView iv;
        ImageLoadTask(ImageView iv) { this.iv = iv; }
        @Override protected Bitmap doInBackground(String... params) {
            try {
                URL u = new URL(params[0]);
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setDoInput(true);
                c.connect();
                InputStream is = c.getInputStream();
                Bitmap b = BitmapFactory.decodeStream(is);
                is.close();
                return b;
            } catch (Exception e) { return null; }
        }
        @Override protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) iv.setImageBitmap(bitmap);
        }
    }

    private static class ImageLoadButtonTask extends AsyncTask<String, Void, Bitmap> {
        private final ImageButton ib;
        ImageLoadButtonTask(ImageButton ib) { this.ib = ib; }
        @Override protected Bitmap doInBackground(String... params) {
            try {
                URL u = new URL(params[0]);
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setDoInput(true);
                c.connect();
                InputStream is = c.getInputStream();
                Bitmap b = BitmapFactory.decodeStream(is);
                is.close();
                return b;
            } catch (Exception e) { return null; }
        }
        @Override protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) ib.setImageBitmap(bitmap);
        }
    }
}
