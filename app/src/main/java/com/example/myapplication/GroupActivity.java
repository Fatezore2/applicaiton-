package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GroupActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GroupAdapter adapter;
    private List<GroupItem> groupList = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid;

    private Button btnAddGroup, btnDeleteGroup, btnJoinGroup;
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_group);

        recyclerView = findViewById(R.id.recyclerDialogChat);
        btnAddGroup = findViewById(R.id.btnAddgroup);
        btnDeleteGroup = findViewById(R.id.btnDeleteGroup);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new GroupAdapter(groupList, group -> {
            // 點擊群組項目時，進入聊天室
            Intent intent = new Intent(GroupActivity.this, ChatMessageActivity.class);
            intent.putExtra("GROUP_ID", group.getId());
            intent.putExtra("GROUP_NAME", group.getName());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        listenForGroups();

        btnAddGroup.setOnClickListener(v -> showCreateGroupDialog());
        btnDeleteGroup.setOnClickListener(v -> showLeaveGroupDialog());
    }

    // ================= 從 users 文件讀取群組 =================
    private void listenForGroups() {
        Log.d("GroupActivity", "開始監聽用戶群組，UID: " + uid);

        userListener = db.collection("users")
                .document(uid)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e("GroupActivity", "監聽錯誤", error);
                        return;
                    }

                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        Log.d("GroupActivity", "用戶文件不存在");
                        groupList.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    // 🔴 取得 groups 物件（Map 格式）
                    Object groupsObj = documentSnapshot.get("groups");

                    if (groupsObj == null) {
                        Log.d("GroupActivity", "沒有群組");
                        groupList.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    if (!(groupsObj instanceof Map)) {
                        Log.e("GroupActivity", "groups 不是 Map 類型");
                        return;
                    }

                    Map<String, Object> groupsMap = (Map<String, Object>) groupsObj;
                    Log.d("GroupActivity", "找到 " + groupsMap.size() + " 個群組");

                    // 清空列表
                    groupList.clear();

                    // 使用 AtomicInteger 追蹤載入完成數量
                    AtomicInteger pendingCount = new AtomicInteger(groupsMap.size());
                    List<GroupItem> tempList = new ArrayList<>();

                    if (groupsMap.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    // 遍歷所有群組
                    for (Map.Entry<String, Object> entry : groupsMap.entrySet()) {
                        String groupId = entry.getKey();
                        Object groupDataObj = entry.getValue();

                        if (!(groupDataObj instanceof Map)) {
                            Log.e("GroupActivity", "群組資料格式錯誤: " + groupId);
                            if (pendingCount.decrementAndGet() == 0) {
                                updateGroupList(tempList);
                            }
                            continue;
                        }

                        Map<String, Object> groupData = (Map<String, Object>) groupDataObj;
                        String groupName = (String) groupData.get("groupName");
                        String role = (String) groupData.get("role");

                        Log.d("GroupActivity", "處理群組: " + groupName + " (ID: " + groupId + ")");

                        // 獲取成員數量
                        db.collection("groups")
                                .document(groupId)
                                .collection("members")
                                .get()
                                .addOnSuccessListener(membersSnapshot -> {
                                    int memberCount = membersSnapshot.size();
                                    Log.d("GroupActivity", "群組 " + groupName + " 有 " + memberCount + " 位成員");

                                    // 獲取群組 ownerId
                                    db.collection("groups")
                                            .document(groupId)
                                            .get()
                                            .addOnSuccessListener(groupDoc -> {
                                                String ownerId = groupDoc.getString("createdBy");

                                                GroupItem item = new GroupItem(
                                                        groupId,
                                                        groupName != null ? groupName : "未命名群組",
                                                        memberCount,
                                                        role,
                                                        ownerId
                                                );
                                                tempList.add(item);

                                                if (pendingCount.decrementAndGet() == 0) {
                                                    updateGroupList(tempList);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                // 如果無法獲取 ownerId，仍然加入群組
                                                GroupItem item = new GroupItem(
                                                        groupId,
                                                        groupName != null ? groupName : "未命名群組",
                                                        memberCount,
                                                        role,
                                                        null
                                                );
                                                tempList.add(item);

                                                if (pendingCount.decrementAndGet() == 0) {
                                                    updateGroupList(tempList);
                                                }
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("GroupActivity", "獲取成員數量失敗: " + groupId, e);

                                    // 即使失敗也要加入基本資料
                                    GroupItem item = new GroupItem(
                                            groupId,
                                            groupName != null ? groupName : "未命名群組",
                                            1,  // 預設至少 1 位成員（自己）
                                            role != null ? role : "member",
                                            null
                                    );
                                    tempList.add(item);

                                    if (pendingCount.decrementAndGet() == 0) {
                                        updateGroupList(tempList);
                                    }
                                });
                    }
                });
    }

    private void updateGroupList(List<GroupItem> tempList) {
        groupList.clear();
        groupList.addAll(tempList);

        // 依群組名稱排序
        Collections.sort(groupList, (a, b) -> a.getName().compareTo(b.getName()));

        adapter.notifyDataSetChanged();
        Log.d("GroupActivity", "群組列表更新完成，共 " + groupList.size() + " 個群組");
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

        Map<String, Object> groupData = new HashMap<>();
        groupData.put("name", name);
        groupData.put("createdBy", uid);
        groupData.put("joinCode", joinCode);
        groupData.put("memberCount", 1);
        groupData.put("createdAt", FieldValue.serverTimestamp());

        db.collection("groups")
                .add(groupData)
                .addOnSuccessListener(docRef -> {
                    String groupId = docRef.getId();

                    // 將建立者加入 members 子集合
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("name", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                    memberData.put("role", "admin");
                    memberData.put("joinedAt", FieldValue.serverTimestamp());
                    memberData.put("status", "online");

                    db.collection("groups")
                            .document(groupId)
                            .collection("members")
                            .document(uid)
                            .set(memberData);

                    // 更新用戶的 groups 物件
                    Map<String, Object> newGroup = new HashMap<>();
                    newGroup.put("groupId", groupId);
                    newGroup.put("groupName", name);
                    newGroup.put("joinedAt", FieldValue.serverTimestamp());
                    newGroup.put("lastRead", FieldValue.serverTimestamp());
                    newGroup.put("role", "admin");

                    db.collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                Map<String, Object> groups = new HashMap<>();
                                if (userDoc.exists() && userDoc.contains("groups")) {
                                    Object existingGroups = userDoc.get("groups");
                                    if (existingGroups instanceof Map) {
                                        groups = (Map<String, Object>) existingGroups;
                                    }
                                }
                                groups.put(groupId, newGroup);

                                db.collection("users")
                                        .document(uid)
                                        .update("groups", groups, "currentGroupId", groupId);
                            });

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
        String groupId = group.getId();

        // 如果是群主，刪除整個群組
        if (uid.equals(group.getOwnerId())) {
            // 先刪除 members 子集合
            db.collection("groups")
                    .document(groupId)
                    .collection("members")
                    .get()
                    .addOnSuccessListener(membersSnapshot -> {
                        WriteBatch batch = db.batch();

                        for (DocumentSnapshot memberDoc : membersSnapshot) {
                            batch.delete(memberDoc.getReference());
                        }

                        // 最後刪除群組文件
                        batch.delete(db.collection("groups").document(groupId));

                        batch.commit().addOnSuccessListener(v -> {
                            clearCurrentGroupIfMatch(groupId);
                            Toast.makeText(this, "群組已刪除", Toast.LENGTH_SHORT).show();
                        });
                    });
            return;
        }

        // 普通成員：從 members 子集合中移除自己
        db.collection("groups")
                .document(groupId)
                .collection("members")
                .document(uid)
                .delete()
                .addOnSuccessListener(v -> {
                    // 更新 memberCount
                    db.collection("groups")
                            .document(groupId)
                            .update("memberCount", FieldValue.increment(-1));

                    // 從用戶的 groups 物件中移除
                    db.collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                if (userDoc.exists() && userDoc.contains("groups")) {
                                    Object groupsObj = userDoc.get("groups");
                                    if (groupsObj instanceof Map) {
                                        Map<String, Object> groups = (Map<String, Object>) groupsObj;
                                        groups.remove(groupId);

                                        db.collection("users")
                                                .document(uid)
                                                .update("groups", groups);
                                    }
                                }
                            });

                    clearCurrentGroupIfMatch(groupId);
                    Toast.makeText(this, "已退出群組", Toast.LENGTH_SHORT).show();
                });
    }

    // 🔥 清除 currentGroupId
    private void clearCurrentGroupIfMatch(String groupId) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String current = doc.getString("currentGroupId");
                        if (groupId.equals(current)) {
                            db.collection("users")
                                    .document(uid)
                                    .update("currentGroupId", FieldValue.delete());
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
        if (userListener != null) userListener.remove();
    }
}