package com.example.myapplication;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class WeatherRepository {

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public interface WeatherCallback {
        void onResult(String weatherData);
        void onError(Exception e);
    }

    public void fetchWeatherData(double lat, double lng, WeatherCallback callback) {

        executor.execute(() -> {

            try {

                String urlString =
                        "https://api.open-meteo.com/v1/forecast?latitude="
                                + lat +
                                "&longitude=" + lng +
                                "&current_weather=true";

                java.net.URL url = new java.net.URL(urlString);

                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                java.io.BufferedReader reader =
                        new java.io.BufferedReader(
                                new java.io.InputStreamReader(conn.getInputStream())
                        );

                StringBuilder json = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }

                reader.close();

                org.json.JSONObject obj =
                        new org.json.JSONObject(json.toString());

                org.json.JSONObject current =
                        obj.getJSONObject("current_weather");

                double temp = current.getDouble("temperature");

                String result = "🌤 " + temp + "°C";

                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onResult(result));

            } catch (Exception e) {

                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e));
            }

        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}