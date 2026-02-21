package com.example.myapplication;

import java.util.HashMap;
import java.util.Map;

public class MCVoice extends MContent {
    private String audioUrl;   // Firebase Storage URL
    private int duration;      // in seconds

    public MCVoice(String audioUrl, int duration) {
        this.audioUrl = audioUrl;
        this.duration = duration;
    }

    public String getAudioUrl() { 
        return audioUrl; 
    }
    public int getDuration() { 
        return duration; 
    }
    @Override
    public String getType() {
        return "voice";
    }

    @Override
    public String getPreview() {
        return "[audioUrl: " + audioUrl + ", duration: " + duration + "s]";
    }
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", getType());
        map.put("audioUrl", audioUrl);
        map.put("duration", duration);
        return map;
    }
    
    public static MCVoice fromMap(Map<String, Object> data) {
        String audioUrl = (String) data.get("audioUrl");
        int duration = (int) data.get("duration");
        return new MCVoice(audioUrl, duration);
    }
}