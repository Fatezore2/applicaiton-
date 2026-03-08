package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DestiationActivitysetting extends AppCompatActivity {

    private Button btn_add, btn_settings;
    private ListView listView;
    private DestinationAdapter adapter;

    // 型をMapから独立したDestinationクラスに変更
    private List<Destination> destinationList = new ArrayList<>();
    private List<String> destinationIds = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_desinaitonsetting);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        bindViews();
        setupListeners();
        setupList();
        loadGroupId();
    }

    private void bindViews() {
        btn_add = findViewById(R.id.btn_add);
        btn_settings = findViewById(R.id.btn_settings);
        listView = findViewById(R.id.list_view_main);
    }

    private void setupListeners() {
        btn_add.setOnClickListener(v ->
                startActivity(new Intent(this, ItemDestinationActivity.class))
        );
        btn_settings.setOnClickListener(v ->
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupList() {
        // ここでdestinationList (List<Destination>)を渡す
        adapter = new DestinationAdapter(this, destinationList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String destinationId = destinationIds.get(position);
            showOptionsDialog(destinationId);
        });
    }

    private void showOptionsDialog(String destinationId) {
        String[] options = {"Update", "Delete"};
        new AlertDialog.Builder(this)
                .setTitle("Destination Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openUpdate(destinationId);
                    else confirmDelete(destinationId);
                })
                .show();
    }

    private void openUpdate(String destinationId) {
        Intent intent = new Intent(this, ItemDestinationActivity.class);
        intent.putExtra("destinationId", destinationId);
        startActivity(intent);
    }

    private void confirmDelete(String destinationId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Destination")
                .setMessage("Are you sure you want to delete this destination?")
                .setPositiveButton("Delete", (dialog, which) -> deleteDestination(destinationId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDestination(String destinationId) {
        if (groupId == null) return;

        db.collection("groups").document(groupId).collection("destinations")
                .document(destinationId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Destination Deleted", Toast.LENGTH_SHORT).show();
                    loadDestinations();
                });
    }

    private void loadGroupId() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        groupId = doc.getString("currentGroupId");
                        if (groupId != null) loadDestinations();
                    }
                });
    }

    private void loadDestinations() {
        if (groupId == null) return;

        db.collection("groups").document(groupId).collection("destinations")
                .orderBy("dateTime").get()
                .addOnSuccessListener(snapshot -> {
                    destinationList.clear();
                    destinationIds.clear();

                    for (DocumentSnapshot doc : snapshot) {
                        // ここでtoObjectを使用して型安全に変換
                        Destination dest = doc.toObject(Destination.class);
                        if (dest != null) {
                            destinationList.add(dest);
                            destinationIds.add(doc.getId());
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (groupId != null) loadDestinations();
    }
}