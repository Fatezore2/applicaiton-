package com.example.myapplication;

import java.util.HashMap;
import java.util.Map;

public class MCImage extends MContent {
    private String imageUrl;
    
    public MCImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    @Override
    public String getType() {
        return "image";
    }
    @Override
    public String getPreview() { 
        return "[imageUrl: " + imageUrl + "]"; 
    }
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", getType());
        map.put("imageUrl", imageUrl);
        return map;
    }
    public static MCImage fromMap(Map<String, Object> data) {
        String imageUrl = (String) data.get("imageUrl");
        return new MCImage(imageUrl);
    }

}