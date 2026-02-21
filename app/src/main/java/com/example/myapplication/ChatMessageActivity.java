package com.example.myapplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatmessage);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        bindViews();
        setupRecycler();
        setupListeners();

        // Get group ID from intent
        groupId = getIntent().getStringExtra("GROUP_ID");

        if (groupId != null) {
            loadGroupInfo();
            listenForMessages();
        } else {
            loadGroupFromUser();
        }
    }

    // ================= BIND UI =================
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
    }

    // ================= LOAD GROUP FROM USER PROFILE =================
    private void loadGroupFromUser() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
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

    // ================= LOAD GROUP INFO =================
    private void loadGroupInfo() {
        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    groupName = doc.getString("name");
                    joinCode = doc.getString("joinCode");

                    tvGroupName.setText(groupName != null ? groupName : "群組聊天室");

                    if (joinCode != null) {
                        tvJoinCode.setText("群組加入碼：" + joinCode);
                    }

                    // Tap to copy join code
                    tvJoinCode.setOnClickListener(v -> {
                        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("JOIN_CODE", joinCode));
                        Toast.makeText(this, "加入碼已複製", Toast.LENGTH_SHORT).show();
                    });
                });
    }

    // ================= CHAT LISTENER =================
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

                        String id = doc.getId();
                        String senderId = (String) data.get("senderId");
                        String senderName = (String) data.get("senderName");
                        Timestamp timestamp = (Timestamp) data.get("timestamp");
                        MContent content = MContent.fromMap(data);

                        if (content == null) continue;

                        messageList.add(new ChatMessage(id, senderId, senderName, timestamp, content));
                    }

                    chatAdapter.notifyDataSetChanged();
                    recyclerChat.scrollToPosition(messageList.size() - 1);
                });
    }

    // ================= SEND MESSAGE =================
    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String senderName = doc.getString("username");
                    if (senderName == null) senderName = "匿名";

                    MCText content = new MCText(text);

                    ChatMessage msg = new ChatMessage(
                            null,
                            uid,
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

    // ================= INVITE DIALOG =================
    private void showInviteDialog() {
        if (joinCode == null) return;

        new AlertDialog.Builder(this)
                .setTitle("邀請成員")
                .setMessage("請分享此加入碼給朋友：\n\n" + joinCode)
                .setPositiveButton("OK", null)
                .show();
    }

    // ================= LISTENERS =================
    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendMessage());
        btnInvite.setOnClickListener(v -> showInviteDialog());
    }

    // ================= CLEAN UP =================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListener != null) chatListener.remove();
    }
}
