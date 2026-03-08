package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;

public class GroupMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private String groupId;
    private List<LatLng> routePoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_map); // 請確保你有對應的 XML

        db = FirebaseFirestore.getInstance();
        groupId = getIntent().getStringExtra("groupId");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.groupMap);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        loadDestinationsAndDrawRoute();
    }

    private void loadDestinationsAndDrawRoute() {
        if (groupId == null) return;

        // 核心邏輯：依照 dateTime 升序排列 (ASCENDING)
        db.collection("groups").document(groupId)
                .collection("destinations")
                .orderBy("dateTime", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    mMap.clear();
                    routePoints.clear();
                    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

                    for (QueryDocumentSnapshot doc : value) {
                        double lat = doc.getDouble("lat");
                        double lng = doc.getDouble("lng");
                        String city = doc.getString("city");
                        LatLng pos = new LatLng(lat, lng);

                        routePoints.add(pos);
                        boundsBuilder.include(pos);

                        // 加 Marker
                        mMap.addMarker(new MarkerOptions()
                                .position(pos)
                                .title(city)
                                .snippet(doc.getDate("dateTime").toString()));
                    }

                    if (!routePoints.isEmpty()) {
                        drawRoute();
                        // 自動縮放地圖以包含所有點
                        int padding = 150; // 邊距
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), padding));
                    }
                });
    }

    private void drawRoute() {
        // 設定箭頭樣式 (需一張箭頭圖片放在 drawable，或使用內建的 CustomCap)
        PolylineOptions options = new PolylineOptions()
                .addAll(routePoints)
                .width(12)
                .color(Color.parseColor("#2EC4C1"))
                .geodesic(true)
                .clickable(true);

        Polyline polyline = mMap.addPolyline(options);

        // 設定線段終點為箭頭 (這能讓使用者知道下一站去哪)
        polyline.setEndCap(new RoundCap());
        polyline.setJointType(JointType.ROUND);
    }
}