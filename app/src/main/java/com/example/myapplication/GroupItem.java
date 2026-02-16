package com.example.myapplication;

import java.util.List;

public class GroupItem {
    private String id;
    private String name;
    private List<String> members;
    private String ownerId;

    // Constructor
    public GroupItem(String id, String name, List<String> members, String ownerId) {
        this.id = id;
        this.name = name;
        this.members = members;
        this.ownerId = ownerId;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getMembers() {
        return members;
    }

    public String getOwnerId() {
        return ownerId;
    }
}
