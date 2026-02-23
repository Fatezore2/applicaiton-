package com.example.myapplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatMessageActivity extends AppCompatActivity {

    // UI
    private TextView tvGroupName;
    private ImageButton btnInvite;
    private RecyclerView recyclerChat;
    private EditText etMessage;
    private Button btnSend;
    private TextView tvJoinCode;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration chatListener;

    // Data
    private String groupId;
    private String groupName;
    private String joinCode;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatmessage);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        bindViews();
        setupRecycler();
        setupImagePicker();
        setupListeners();

        groupId = getIntent().getStringExtra("GROUP_ID");

        if (groupId != null) {
            loadGroupInfo();
            listenForMessages();
        } else {
            loadGroupFromUser();
        }
    }

    // ================= IMAGE PICKER =================

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadImageToFirebase(uri);
                    }
                });
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    // ================= BIND =================

    private void bindViews() {
        tvGroupName = findViewById(R.id.tvGroupName);
        btnInvite = findViewById(R.id.btnInvite);
        recyclerChat = findViewById(R.id.recyclerChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        tvJoinCode = findViewById(R.id.tvJoinCode);
    }

    // ================= RECYCLER =================

    private void setupRecycler() {
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(manager);

        chatAdapter = new ChatAdapter(messageList);
        recyclerChat.setAdapter(chatAdapter);
        chatAdapter.setOnMessageLongClickListener(message -> {
            showDeleteDialog(message);
        });
    }

    // ================= LOAD GROUP =================

    private void loadGroupFromUser() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    groupId = doc.getString("currentGroupId");
                    if (groupId == null) {
                        Toast.makeText(this, "尚未加入群組", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    loadGroupInfo();
                    listenForMessages();
                });
    }

    private void loadGroupInfo() {
        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    groupName = doc.getString("name");
                    joinCode = doc.getString("joinCode");

                    tvGroupName.setText(groupName != null ? groupName : "群組聊天室");

                    if (joinCode != null) {
                        tvJoinCode.setText("群組加入碼：" + joinCode);

                        tvJoinCode.setOnClickListener(v -> {
                            ClipboardManager cm =
                                    (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(
                                    ClipData.newPlainText("JOIN_CODE", joinCode));
                            Toast.makeText(this, "加入碼已複製", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    // ================= LISTEN MESSAGES =================

    private void listenForMessages() {

        if (groupId == null) return;

        if (chatListener != null) chatListener.remove();

        chatListener = db.collection("groups")
                .document(groupId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(this, (snapshots, e) -> {

                    if (e != null || snapshots == null) {
                        Log.e("Chat", "Listen failed", e);
                        return;
                    }

                    messageList.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {

                        Map<String, Object> data = doc.getData();
                        if (data == null) continue;

                        Timestamp timestamp = doc.getTimestamp("timestamp");

                        // ignore temporary null timestamp
                        if (timestamp == null) continue;

                        MContent content = MContent.fromMap(data);
                        if (content == null) continue;

                        messageList.add(new ChatMessage(
                                doc.getId(),
                                doc.getString("senderId"),
                                doc.getString("senderName"),
                                timestamp,
                                content
                        ));
                    }

                    chatAdapter.notifyDataSetChanged();

                    if (!messageList.isEmpty()) {
                        recyclerChat.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    // ================= SEND TEXT =================

    private void sendMessage() {

        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {

                    String senderName = doc.getString("username");
                    if (senderName == null) senderName = "匿名";

                    MCText content = new MCText(text);

                    ChatMessage msg = new ChatMessage(
                            null,
                            user.getUid(),
                            senderName,
                            null,
                            content
                    );

                    db.collection("groups")
                            .document(groupId)
                            .collection("messages")
                            .add(msg.toMap())
                            .addOnSuccessListener(r -> etMessage.setText(""));
                });
    }

    // ================= SEND IMAGE =================

    private void uploadImageToFirebase(Uri uri) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null || groupId == null) return;

        Toast.makeText(this, "圖片上傳中...", Toast.LENGTH_SHORT).show();

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child("chat_images")
                .child(groupId)
                .child(System.currentTimeMillis() + ".jpg");

        ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(downloadUrl ->
                                sendImageMessage(downloadUrl.toString())))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "圖片上傳失敗", Toast.LENGTH_SHORT).show());
    }

    private void sendImageMessage(String imageUrl) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {

                    String senderName = doc.getString("username");
                    if (senderName == null) senderName = "匿名";

                    MCImage content = new MCImage(imageUrl);

                    ChatMessage msg = new ChatMessage(
                            null,
                            user.getUid(),
                            senderName,
                            null,
                            content
                    );

                    db.collection("groups")
                            .document(groupId)
                            .collection("messages")
                            .add(msg.toMap());
                });
    }

    // ================= LISTENERS =================

    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendMessage());
        btnInvite.setOnClickListener(v -> openImagePicker());
    }

    // ================= CLEAN UP =================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListener != null) chatListener.remove();
    }
    private void showDeleteDialog(ChatMessage message) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // 只允許刪除自己訊息
        if (!user.getUid().equals(message.getSenderId())) {
            Toast.makeText(this, "他人のメッセージは削除できません", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("メッセージ削除")
                .setMessage("このメッセージを削除しますか？")
                .setPositiveButton("削除", (dialog, which) -> deleteMessage(message))
                .setNegativeButton("キャンセル", null)
                .show();
    }
    private void deleteMessage(ChatMessage message) {

        if (groupId == null || message.getId() == null) return;

        db.collection("groups")
                .document(groupId)
                .collection("messages")
                .document(message.getId())
                .delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "削除しました", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "削除失敗", Toast.LENGTH_SHORT).show()
                );
    }
}