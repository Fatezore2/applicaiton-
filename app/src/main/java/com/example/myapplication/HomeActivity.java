package com.example.myapplication;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ImageView wether;
    private ListenerRegistration destinationListener;
    private Map<String, Marker> destinationMarkers = new HashMap<>();
    private List<DocumentSnapshot> destinationDocs = new ArrayList<>();
    private Handler countdownHandler = new Handler(Looper.getMainLooper());
    private Runnable countdownRunnable;
    private TextView DESwether;

    private List<DestinationItem> destinationList = new ArrayList<>();
    private Handler timeHandler = new Handler();
    private Runnable timeRunnable;
    private Polyline routePolyline;
    private boolean showOnlineOnly = false;
    private TextView tvLogo, tvTripTitle, tvDestinationTitle;
    private Button btnOpenGroup, btnJoinGroup, btnMoreOptions, btnMyLocation;
    private ImageView setting,locame;
    private GoogleMap mMap;
    private boolean showMyLocation = true;
    private String currentGroupId;
    private String uid;
    private Map<String, Marker> memberMarkers = new HashMap<>();
    private static final int REQ_LOCATION = 1001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        startLocationUpdates();
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        uid = auth.getUid();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        createNotificationChannel();
        bindViews();
        setupListeners();
        checkLocationPermission();
        loadCurrentGroup();

    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {

        LocationRequest request = LocationRequest.create();
        request.setInterval(5000); // 5秒更新一次
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;

                Location location = result.getLastLocation();
                if (location != null && currentGroupId != null) {

                    Map<String, Object> data = new HashMap<>();
                    data.put("lat", location.getLatitude());
                    data.put("lng", location.getLongitude());

                    db.collection("users")
                            .document(uid)
                            .update(data);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                getMainLooper()
        );
    }
    private void startCountdownUpdater() {

        if (countdownRunnable != null)
            countdownHandler.removeCallbacks(countdownRunnable);

        countdownRunnable = new Runnable() {
            @Override
            public void run() {

                updateDestinationMarkers();
                countdownHandler.postDelayed(this, 1000);
            }
        };

        countdownHandler.post(countdownRunnable);
    }
    private void updateNextDestinationText() {

        if (destinationDocs.isEmpty()) {
            tvDestinationTitle.setText("尚未設定行程");
            return;
        }

        long now = System.currentTimeMillis();

        DocumentSnapshot nextDoc = null;

        for (DocumentSnapshot doc : destinationDocs) {

            Date date = doc.getDate("dateTime");
            if (date == null) continue;

            if (date.getTime() > now) {
                nextDoc = doc;
                break;
            }
        }

        if (nextDoc == null) {
            tvDestinationTitle.setText("🎉 今日行程完成");
            return;
        }

        String city = nextDoc.getString("city");
        Date date = nextDoc.getDate("dateTime");

        long diff = date.getTime() - now;

        long hours = diff / (1000 * 60 * 60);
        long minutes = (diff / (1000 * 60)) % 60;

        tvDestinationTitle.setText(
                "📍 Next Stop: " + city +
                        "\n⏳ " + hours + "h " + minutes + "m remaining"
        );
    }
    private void bindViews() {
        wether = findViewById(R.id.wether);
        tvTripTitle = findViewById(R.id.tvTripTitle);
        tvDestinationTitle = findViewById(R.id.tvDestinationTitle);
        btnOpenGroup = findViewById(R.id.btnOpenGroup);
        btnJoinGroup = findViewById(R.id.getgrounp);
        btnMoreOptions = findViewById(R.id.btnMoreOptions);
        setting = findViewById(R.id.setting);
        locame = findViewById(R.id.locame);
        ToggleButton toggleOnline = findViewById(R.id.toggleOnline);
        DESwether = findViewById(R.id.DESwether);
        ToggleButton toggleMyLocation = findViewById(R.id.toggleOnline);

        toggleMyLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {

            showMyLocation = isChecked;

            if (mMap == null) return;

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                mMap.setMyLocationEnabled(showMyLocation);
            }
        });
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }
    private void loadDestinationWeather() {

        if (destinationDocs.isEmpty()) {
            DESwether.setText("No destinations");
            return;
        }

        new Thread(() -> {

            try {

                StringBuilder result = new StringBuilder();

                int limit = Math.min(3, destinationDocs.size());

                for (int i = 0; i < limit; i++) {

                    DocumentSnapshot doc = destinationDocs.get(i);

                    String city = doc.getString("city");
                    Double lat = doc.getDouble("lat");
                    Double lng = doc.getDouble("lng");

                    if (lat == null || lng == null) continue;

                    String url =
                            "https://api.open-meteo.com/v1/forecast?latitude="
                                    + lat +
                                    "&longitude=" + lng +
                                    "&daily=weathercode&timezone=auto";

                    java.net.URL u = new java.net.URL(url);

                    java.net.HttpURLConnection conn =
                            (java.net.HttpURLConnection) u.openConnection();

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

                    int code =
                            obj.getJSONObject("daily")
                                    .getJSONArray("weathercode")
                                    .getInt(0);

                    String emoji = getWeatherEmoji(code);

                    result.append(emoji)
                            .append(" ")
                            .append(city);

                    if (i < limit - 1) {
                        result.append("  →  ");
                    }
                }

                runOnUiThread(() ->
                        DESwether.setText(result.toString())
                );

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }
    private String getWeatherEmoji(int code) {

        if (code == 0) return "☀";
        if (code == 1 || code == 2) return "🌤";
        if (code == 3) return "☁";
        if (code <= 48) return "🌫";
        if (code <= 67) return "🌧";
        if (code <= 77) return "❄";
        if (code <= 99) return "⛈";

        return "☁";
    }
    private void setupListeners() {
        locame.setOnClickListener(v -> moveToMyLocation());
        btnOpenGroup.setOnClickListener(v ->
                startActivity(new Intent(this, GroupActivity.class)));

        btnJoinGroup.setOnClickListener(v -> showJoinDialog());
        setting.setOnClickListener(v -> {
            startActivity(new Intent(this, settingActivity.class));
        });
        btnMoreOptions.setOnClickListener(v -> {
            if (currentGroupId == null) {
                toast("請先加入群組");
                return;
            }
            startActivity(new Intent(this, DestiationActivitysetting.class));
        });

        // ===== 新增：飯店搜尋按鈕點擊事件 =====
        Button btnHotelSearch = findViewById(R.id.btnHotelSearch);
        btnHotelSearch.setOnClickListener(v -> {
            // 開啟飯店搜尋 Activity
            Intent intent = new Intent(HomeActivity.this, HotelSearchActivity.class);

            // 如果有地圖中心點，可以傳過去
            if (mMap != null) {
                LatLng center = mMap.getCameraPosition().target;
                intent.putExtra("latitude", center.latitude);
                intent.putExtra("longitude", center.longitude);
            }

            startActivity(intent);
        });
        wether.setOnClickListener(v -> {

            if (destinationDocs.isEmpty()) {
                toast("No destination selected");
                return;
            }

            DocumentSnapshot doc = destinationDocs.get(0);

            Double lat = doc.getDouble("lat");
            Double lng = doc.getDouble("lng");
            String city = doc.getString("city");

            if (lat != null && lng != null) {
                showWeatherDialog(lat, lng, city);
            }

        });
    }
    private void showWeatherDialog(double lat, double lng, String city) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(
                R.layout.dialog_weather,
                null
        );

        LinearLayout container =
                view.findViewById(R.id.weatherContainer);

        builder.setTitle("7 Day Weather - " + city);
        builder.setView(view);
        builder.setPositiveButton("Close", null);

        builder.show();

        loadWeatherData(lat, lng, container);
    }
    private void loadWeatherData(double lat, double lng, LinearLayout container) {

        String url =
                "https://api.open-meteo.com/v1/forecast?latitude="
                        + lat +
                        "&longitude=" + lng +
                        "&daily=weathercode,temperature_2m_max&timezone=auto";

        new Thread(() -> {

            try {

                java.net.URL u = new java.net.URL(url);

                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) u.openConnection();

                java.io.BufferedReader reader =
                        new java.io.BufferedReader(
                                new java.io.InputStreamReader(
                                        conn.getInputStream()
                                )
                        );

                StringBuilder json = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }

                reader.close();

                org.json.JSONObject obj =
                        new org.json.JSONObject(json.toString());

                org.json.JSONObject daily =
                        obj.getJSONObject("daily");

                org.json.JSONArray temps =
                        daily.getJSONArray("temperature_2m_max");

                org.json.JSONArray times =
                        daily.getJSONArray("time");

                runOnUiThread(() -> {

                    for (int i = 0; i < temps.length(); i++) {

                        try {

                            View item =
                                    getLayoutInflater().inflate(
                                            R.layout.item_weather,
                                            container,
                                            false
                                    );

                            TextView day =
                                    item.findViewById(R.id.dayText);

                            TextView temp =
                                    item.findViewById(R.id.tempText);

                            TextView weather =
                                    item.findViewById(R.id.weatherText);

                            day.setText(times.getString(i));
                            temp.setText(temps.getDouble(i) + "°C");
                            weather.setText("Forecast");

                            container.addView(item);

                        } catch (Exception ignored) {}
                    }

                });

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }
    @SuppressLint("MissingPermission")
    private void moveToMyLocation() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {

                    if (location == null || mMap == null) return;

                    LatLng myLocation = new LatLng(
                            location.getLatitude(),
                            location.getLongitude()
                    );

                    mMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(myLocation, 17f)
                    );
                });
    }
    private ListenerRegistration membersListener;

    private void listenToGroupMembers(String groupId) {

        if (membersListener != null) membersListener.remove();

        membersListener = db.collection("users")
                .whereEqualTo("currentGroupId", groupId)
                .addSnapshotListener((snap, e) -> {

                    if (e != null || snap == null || mMap == null) return;

                    for (DocumentSnapshot doc : snap.getDocuments()) {

                        String memberUid = doc.getId();
                        Double lat = doc.getDouble("lat");
                        Double lng = doc.getDouble("lng");
                        String username = doc.getString("username");
                        String imageUrl = doc.getString("profileImageUrl");

                        Boolean isOnline = doc.getBoolean("online");

                        if (lat == null || lng == null) continue;

                        // 👇 如果只顯示在線
                        if (showOnlineOnly && (isOnline == null || !isOnline)) {
                            removeMarker(memberUid);
                            continue;
                        }

                        LatLng position = new LatLng(lat, lng);

                        if (memberMarkers.containsKey(memberUid)) {
                            animateMarker(memberMarkers.get(memberUid), position);
                        } else {
                            addMarkerWithImage(memberUid, position, username, imageUrl);
                        }
                    }
                });
    }
    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    "destination_alert",
                    "Destination Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }
    private void animateMarker(Marker marker, LatLng toPosition) {

        LatLng start = marker.getPosition();

        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(1000);

        animator.addUpdateListener(animation -> {

            float v = (float) animation.getAnimatedValue();

            double lat = v * toPosition.latitude +
                    (1 - v) * start.latitude;

            double lng = v * toPosition.longitude +
                    (1 - v) * start.longitude;

            marker.setPosition(new LatLng(lat, lng));
        });

        animator.start();
    }
    private void setupDestinationInteractions() {

        mMap.setOnMarkerClickListener(marker -> {

            Object tag = marker.getTag();
            if (tag == null) return false;

            String destinationId = tag.toString();

            Intent intent = new Intent(this, ItemDestinationActivity.class);
            intent.putExtra("destinationId", destinationId);
            startActivity(intent);

            return true;
        });

        mMap.setOnMarkerDragListener(null);

        mMap.setOnInfoWindowLongClickListener(marker -> {

            Object tag = marker.getTag();
            if (tag == null) return;

            String destinationId = tag.toString();

            new AlertDialog.Builder(this)
                    .setTitle("刪除目的地?")
                    .setPositiveButton("刪除", (d, w) -> {

                        db.collection("groups")
                                .document(currentGroupId)
                                .collection("destinations")
                                .document(destinationId)
                                .delete();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }
    private ListenerRegistration userListener;
    private ListenerRegistration groupListener;

    private void loadCurrentGroup() {

        if (uid == null) return;

        userListener = db.collection("users")
                .document(uid)
                .addSnapshotListener((doc, e) -> {

                    if (e != null || doc == null) return;

                    String newGroupId = doc.getString("currentGroupId");

                    if (TextUtils.isEmpty(newGroupId)) {
                        currentGroupId = null;
                        tvTripTitle.setText("尚未加入群組");

                        // remove old group listener
                        if (groupListener != null) groupListener.remove();
                        return;
                    }

                    if (!newGroupId.equals(currentGroupId)) {
                        currentGroupId = newGroupId;
                        listenToGroupName(newGroupId);
                    }
                });
    }
    private void listenToGroupName(String groupId) {
        listenToGroupMembers(groupId);
        listenToDestinations(groupId);   // 🔥 NEW


        if (groupListener != null) groupListener.remove();

        groupListener = db.collection("groups")
                .document(groupId)
                .addSnapshotListener((doc, e) -> {

                    if (e != null || doc == null || !doc.exists()) {
                        tvTripTitle.setText("尚未加入群組");
                        return;
                    }

                    String groupName = doc.getString("name");

                    if (!TextUtils.isEmpty(groupName)) {
                        tvTripTitle.setText(groupName);
                    } else {
                        tvTripTitle.setText("未命名群組");
                    }
                });
    }
    private void listenToDestinations(String groupId) {

        if (destinationListener != null) destinationListener.remove();

        destinationListener = db.collection("groups")
                .document(groupId)
                .collection("destinations")
                .orderBy("dateTime", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {

                    if (snap == null || mMap == null) return;
                    destinationDocs.clear();
                    destinationDocs.addAll(snap.getDocuments());

                    updateDestinationMarkers();

                    loadDestinationWeather();   // ⭐ 加這行

                    startCountdownUpdater();
                });
    }
    private String getCountdownText(Date date) {

        if (date == null) return "";

        long diff = date.getTime() - System.currentTimeMillis();

        if (diff <= 0) return " (Started)";

        long days = diff / (1000 * 60 * 60 * 24);
        long hours = (diff / (1000 * 60 * 60)) % 24;
        long minutes = (diff / (1000 * 60)) % 60;

        if (days > 0)
            return String.format(" (%d d %d h)", days, hours);
        else if (hours > 0)
            return String.format(" (%d h %d m)", hours, minutes);
        else
            return String.format(" (%d m)", minutes);
    }
    private void updateDestinationMarkers() {

        for (Marker m : destinationMarkers.values()) {
            m.remove();
        }
        destinationMarkers.clear();

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {

                    List<LatLng> routePoints = new ArrayList<>();

                    // 1️⃣ User location
                    if (location != null) {

                        LatLng myLocation = new LatLng(
                                location.getLatitude(),
                                location.getLongitude()
                        );

                        routePoints.add(myLocation);
                    }

                    // 2️⃣ Destinations
                    for (DocumentSnapshot doc : destinationDocs) {

                        String docId = doc.getId();
                        Double lat = doc.getDouble("lat");
                        Double lng = doc.getDouble("lng");
                        String city = doc.getString("city");
                        Date date = doc.getDate("dateTime");

                        if (lat == null || lng == null) continue;

                        LatLng pos = new LatLng(lat, lng);
                        routePoints.add(pos);

                        String countdownText = getCountdownText(date);

                        Marker marker = mMap.addMarker(
                                new MarkerOptions()
                                        .position(pos)
                                        .title("📍 " + city + countdownText)
                                        .icon(BitmapDescriptorFactory
                                                .defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        );

                        marker.setTag(docId);
                        destinationMarkers.put(docId, marker);
                    }

                    // 3️⃣ Draw route
                    if (routePolyline != null) routePolyline.remove();

                    if (routePoints.size() > 1) {

                        routePolyline = mMap.addPolyline(
                                new PolylineOptions()
                                        .addAll(routePoints)
                                        .width(10f)
                                        .color(0xFF2196F3)
                                        .geodesic(true)
                        );
                    }

                    updateNextDestinationText();
                });
    }
    private void drawDestinationRoute(List<LatLng> routePoints) {

        for (DocumentSnapshot doc : destinationDocs) {

            Double lat = doc.getDouble("lat");
            Double lng = doc.getDouble("lng");

            if (lat == null || lng == null) continue;

            routePoints.add(new LatLng(lat, lng));
        }

        if (routePolyline != null) routePolyline.remove();

        if (routePoints.size() > 1) {

            routePolyline = mMap.addPolyline(
                    new PolylineOptions()
                            .addAll(routePoints)
                            .width(10f)
                            .color(0xFF2196F3)
                            .geodesic(true)
            );
        }
    }
    private void showJoinDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("輸入群組加入碼");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("加入", (d, w) -> {
            String code = input.getText().toString().trim().toUpperCase();
            if (!TextUtils.isEmpty(code)) {
                joinGroup(code);
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void joinGroup(String code) {

        db.collection("groups")
                .whereEqualTo("joinCode", code)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        toast("Group not found");
                        return;
                    }

                    String groupId = snapshot.getDocuments().get(0).getId();

                    DocumentReference groupRef =
                            db.collection("groups").document(groupId);

                    DocumentReference userRef =
                            db.collection("users").document(uid);

                    db.runBatch(batch -> {
                                if (code.equals(currentGroupId)) {
                                    toast("你已經在這個群組");
                                    return;
                                }
                                batch.update(groupRef,
                                        "members",
                                        FieldValue.arrayUnion(uid));

                                Map<String, Object> data = new HashMap<>();
                                data.put("currentGroupId", groupId);

                                // IMPORTANT FIX
                                batch.set(userRef, data, SetOptions.merge());

                            }).addOnSuccessListener(v ->
                                    toast("Joined successfully"))
                            .addOnFailureListener(e ->
                                    toast("Join failed: " + e.getMessage()));
                });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);

            fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
            ).addOnSuccessListener(location -> {

                if (location != null) {

                    LatLng myLocation = new LatLng(
                            location.getLatitude(),
                            location.getLongitude()
                    );

                    mMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(myLocation, 16f)
                    );
                }
            });
        }
    }
    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (membersListener != null) membersListener.remove();
        if (userListener != null) userListener.remove();
        if (groupListener != null) groupListener.remove();
        if (destinationListener != null) destinationListener.remove();
        if (countdownHandler != null && countdownRunnable != null)
            countdownHandler.removeCallbacks(countdownRunnable);

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (mMap != null) {
                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                    }
                }
            } else {
                toast("需要定位權限才能顯示你的位置");
            }
        }
    }
    private void addMarkerWithImage(String uid,
                                    LatLng position,
                                    String username,
                                    String imageUrl) {

        if (imageUrl == null) {
            Marker marker = mMap.addMarker(
                    new MarkerOptions()
                            .position(position)
                            .title(username)
            );
            memberMarkers.put(uid, marker);
            return;
        }

        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .circleCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap,
                                                Transition<? super Bitmap> transition) {

                        Bitmap smallBitmap =
                                Bitmap.createScaledBitmap(bitmap,
                                        120,
                                        120,
                                        false);

                        Marker marker = mMap.addMarker(
                                new MarkerOptions()
                                        .position(position)
                                        .title(username)
                                        .icon(BitmapDescriptorFactory
                                                .fromBitmap(smallBitmap))
                        );

                        memberMarkers.put(uid, marker);
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                    }
                });
    }
    private void removeMarker(String uid) {
        if (memberMarkers.containsKey(uid)) {
            memberMarkers.get(uid).remove();
            memberMarkers.remove(uid);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        setOnlineStatus(true);
    }
    @Override
    protected void onStop() {
        super.onStop();
        setOnlineStatus(false);
    }

    private void setOnlineStatus(boolean online) {

        if (uid == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("online", online);

        db.collection("users")
                .document(uid)
                .update(data);
    }
    private static class DestinationItem {
        String name;
        int hour;
        int minute;

        public DestinationItem(String name, int hour, int minute) {
            this.name = name;
            this.hour = hour;
            this.minute = minute;
        }
    }
}