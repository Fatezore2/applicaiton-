package com.example.myapplication;

import java.util.HashMap;
import java.util.Map;

public class MCText extends MContent {
    private String text;
    
    public MCText(String text) {
        this.text = text;
    }
    public String getText() {
        return text;
    }
    @Override
    public String getType() {
        return "text";
    }
    @Override
    public String getPreview() { 
        return "[text: " + text + "]"; 
    }
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", getType());
        map.put("text", text);
        return map;
    }

    public static MCText fromMap(Map<String, Object> data) {
        String text = (String) data.get("text");
        return new MCText(text);
    }
}