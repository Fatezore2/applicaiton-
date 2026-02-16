package com.example.myapplication;

import java.util.Map;

public abstract class MContent {
    public abstract String getType();
    public abstract String getPreview();
    public abstract Map<String, Object> toMap();

    public static MContent fromMap(Map<String, Object> data) {
        String type = (String) data.get("type");
        switch (type) {
            case "text": 
                return MCText.fromMap(data);
            case "image": 
                return MCImage.fromMap(data);
            case "voice": 
                return MCVoice.fromMap(data);
            default: return null;
        }
    }

}