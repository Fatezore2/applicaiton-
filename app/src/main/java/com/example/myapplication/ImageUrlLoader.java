package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

class ImageUrlLoader {
    private static final LruCache<String, Bitmap> cache =
            new LruCache<>(50); // store up to 50 bitmaps

    public static void loadImage(String urlString, ImageView imageView) {
        imageView.setImageDrawable(null);
        // Show placeholder first
        imageView.setImageResource(R.drawable.placeholder);

        // Check cache
        Bitmap cached = cache.get(urlString);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                input.close();
                connection.disconnect();

                cache.put(urlString, bitmap);

                new Handler(Looper.getMainLooper()).post(() -> {
                    imageView.setImageBitmap(bitmap);
                });

            } catch (Exception e) {
                e.printStackTrace();
                // Optionally set an error drawable
                new Handler(Looper.getMainLooper()).post(() -> {
                    imageView.setImageResource(R.drawable.net_error);
                });
            }
        }).start();
    }
}