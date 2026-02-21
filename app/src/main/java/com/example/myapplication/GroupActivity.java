package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class GroupActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GroupAdapter adapter;
    private List<GroupItem> groupList = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid;

    private Button btnAddGroup, btnDeleteGroup, btnJoinGroup;

    private ListenerRegistration groupListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_group);

        recyclerView = findViewById(R.id.recyclerDialogChat);
        btnAddGroup = findViewById(R.id.btnAddgroup);
        btnDeleteGroup = findViewById(R.id.btnDeleteGroup);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        adapter = new GroupAdapter(groupList, g ->
                Toast.makeText(this, "Selected: " + g.getName(), Toast.LENGTH_SHORT).show());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        listenForGroups();

        btnAddGroup.setOnClickListener(v -> showCreateGroupDialog());
        btnDeleteGroup.setOnClickListener(v -> showLeaveGroupDialog());
    }

    // ================= LOAD GROUPS =================
    private void listenForGroups() {
        groupListener = db.collection("groups")
                .whereArrayContains("members", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    groupList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        groupList.add(new GroupItem(
                                doc.getId(),
                                doc.getString("name"),
                                (List<String>) doc.get("members"),
                                doc.getString("ownerId")
                        ));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // ================= CREATE GROUP =================
    private void showCreateGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("建立群組");

        EditText input = new EditText(this);
        input.setHint("群組名稱");
        builder.setView(input);

        builder.setPositiveButton("建立", (d, w) -> {
            String name = input.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) createGroup(name);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void createGroup(String name) {
        String joinCode = generateJoinCode();

        Map<String, Object> g = new HashMap<>();
        g.put("name", name);
        g.put("ownerId", uid);
        g.put("joinCode", joinCode);
        g.put("members", Collections.singletonList(uid));
        g.put("createdAt", FieldValue.serverTimestamp());

        db.collection("groups").add(g)
                .addOnSuccessListener(docRef -> {

                    String groupId = docRef.getId();

                    // 🔥 FIX: use set with merge (NOT update)
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("currentGroupId", groupId);

                    db.collection("users")
                            .document(uid)
                            .set(userData, SetOptions.merge());

                    Toast.makeText(this,
                            "群組建立成功\n加入碼: " + joinCode,
                            Toast.LENGTH_LONG).show();
                });
    }

    // ================= LEAVE GROUP =================
    private void showLeaveGroupDialog() {
        if (groupList.isEmpty()) {
            Toast.makeText(this, "沒有群組", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[groupList.size()];
        for (int i = 0; i < groupList.size(); i++)
            names[i] = groupList.get(i).getName();

        new AlertDialog.Builder(this)
                .setTitle("選擇要退出的群組")
                .setItems(names, (d, which) -> leaveGroup(groupList.get(which)))
                .show();
    }

    private void leaveGroup(GroupItem group) {

        // If owner → delete group
        if (uid.equals(group.getOwnerId())) {
            db.collection("groups").document(group.getId())
                    .delete()
                    .addOnSuccessListener(v -> {

                        clearCurrentGroupIfMatch(group.getId());
                        Toast.makeText(this,
                                "群組已刪除 (你是群主)",
                                Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        // Normal member → remove from members[]
        db.collection("groups").document(group.getId())
                .update("members", FieldValue.arrayRemove(uid))
                .addOnSuccessListener(v -> {

                    clearCurrentGroupIfMatch(group.getId());
                    Toast.makeText(this,
                            "已退出群組",
                            Toast.LENGTH_SHORT).show();
                });
    }

    // 🔥 IMPORTANT: clear currentGroupId when leaving
    private void clearCurrentGroupIfMatch(String groupId) {

        DocumentReference userRef =
                db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(doc -> {

            if (doc.exists()) {
                String current = doc.getString("currentGroupId");
                if (groupId.equals(current)) {
                    userRef.update("currentGroupId", FieldValue.delete());
                }
            }
        });
    }

    // ================= JOIN CODE GENERATOR =================
    private String generateJoinCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(r.nextInt(chars.length())));
        }
        return code.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (groupListener != null) groupListener.remove();
    }

    // ================= APPROVE USER =================
    private void approveUser(String groupId, String userId) {

        WriteBatch batch = db.batch();
        DocumentReference groupRef =
                db.collection("groups").document(groupId);
        DocumentReference userRef =
                db.collection("users").document(userId);
        DocumentReference reqRef =
                groupRef.collection("joinRequests").document(userId);

        batch.update(groupRef,
                "members",
                FieldValue.arrayUnion(userId));

        batch.set(userRef,
                Collections.singletonMap("currentGroupId", groupId),
                SetOptions.merge());

        batch.delete(reqRef);

        batch.commit().addOnSuccessListener(v ->
                Toast.makeText(this,
                        "批准成功",
                        Toast.LENGTH_SHORT).show());
    }
}