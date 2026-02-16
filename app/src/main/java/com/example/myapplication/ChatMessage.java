package com.example.myapplication;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ChatMessage {
    private String id;
    private String senderId;
    private String senderName;
    private Timestamp timestamp;
    private MContent content;

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

    public ChatMessage() {}
    public ChatMessage(String id, String senderId, String senderName, Timestamp timestamp, MContent content) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.timestamp = timestamp;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public String getSenderId() {
        return senderId;
    }
    public String getSenderName(){
        return senderName;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public MContent getContent() {
        return content;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("senderId", senderId);
        map.put("senderName", senderName);

        // Crucial fix: use server timestamp if local one is missing
        if (timestamp != null) {
            map.put("timestamp", timestamp);
        } else {
            map.put("timestamp", FieldValue.serverTimestamp());
        }

        map.putAll(content.toMap());
        return map;
    }

    public static ChatMessage fromMap(Map<String, Object> data) {
        String id = (String) data.get("id");
        String senderId = (String) data.get("senderId");
        String senderName = (String) data.get("senderName");
        Timestamp timestamp = (Timestamp) data.get("timestamp");
        MContent content = MContent.fromMap(data);
        return new ChatMessage(id, senderId, senderName,timestamp, content);
    }


    @Override
    public String toString() {
        if (timestamp == null) return "[unknown time]";
        Date date = timestamp.toDate();  // ✅ convert Timestamp → Date
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        String timeStr = sdf.format(date);


        return "[" + senderId + " @ " + timeStr + "]\n" + content.getPreview();
    }
}
