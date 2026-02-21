package com.example.myapplication;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ItemDestinationActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private EditText etDestinationName, etDestinationTime;
    private Button btnSendDestination;
    private MapView mapPick;

    private String groupId;
    private double pickedLat = 0, pickedLng = 0;
    private Marker destMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_destination);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etDestinationName = findViewById(R.id.etDestinationName);
        etDestinationTime = findViewById(R.id.etDestinationTime);
        btnSendDestination = findViewById(R.id.btnSendDestination);
        mapPick = findViewById(R.id.mapPick);

        setupMapPicker();
        loadGroupFromUser();

        btnSendDestination.setOnClickListener(v -> addDestination());
    }

    // ================= LOAD GROUP =================
    private void loadGroupFromUser() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> groupId = doc.getString("currentGroupId"));
    }

    // ================= MAP PICKER =================
    private void setupMapPicker() {
        mapPick.setMultiTouchControls(true);
        mapPick.getController().setZoom(15.0);
        mapPick.getController().setCenter(new GeoPoint(35.681236, 139.767125));

        mapPick.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {

            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                pickedLat = p.getLatitude();
                pickedLng = p.getLongitude();
                showPickedMarker(p);
                autoFillDestinationName(pickedLat, pickedLng);
                toast("📍 Location selected");
                return true;
            }
        }));
    }

    private void showPickedMarker(GeoPoint p) {
        if (destMarker != null) mapPick.getOverlays().remove(destMarker);

        destMarker = new Marker(mapPick);
        destMarker.setPosition(p);
        destMarker.setTitle("🎯 Destination");
        destMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapPick.getOverlays().add(destMarker);
        mapPick.invalidate();
    }

    // ================= AUTO FILL NAME =================
    private void autoFillDestinationName(double lat, double lng) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
                List<Address> list = geocoder.getFromLocation(lat, lng, 1);
                if (list == null || list.isEmpty()) return;

                Address a = list.get(0);
                String district = a.getSubLocality();
                String city = a.getLocality();
                String country = a.getCountryName();

                String place = "";
                if (district != null) place += district + ", ";
                if (city != null) place += city;
                if (place.isEmpty()) place = country;

                String finalPlace = place;

                runOnUiThread(() -> etDestinationName.setText(finalPlace));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ================= SAVE DESTINATION =================
    private void addDestination() {
        String name = etDestinationName.getText().toString().trim();
        String timeText = etDestinationTime.getText().toString().trim();

        if (groupId == null) {
            toast("❌ Group not loaded yet");
            return;
        }

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(timeText)) {
            toast("請輸入目的地與時間");
            return;
        }

        if (pickedLat == 0) {
            toast("請長按地圖選擇目的地");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("time", timeText);
        data.put("lat", pickedLat);
        data.put("lng", pickedLng);
        data.put("createdBy", auth.getUid());
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("groups").document(groupId)
                .collection("destinations")
                .add(data)
                .addOnSuccessListener(r -> toast("✅ Destination saved"))
                .addOnFailureListener(e -> toast("❌ Save failed"));
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
