package com.example.myapplication;

import java.util.List;

public class GroupItem {
    private String id;
    private String name;
    private int memberCount;      // 新增
    private String role;          // 新增
    private String ownerId;       // 新增

    // 原本的建構子（保留以相容舊程式碼）
    public GroupItem(String id, String name, List<String> members, String ownerId) {
        this.id = id;
        this.name = name;
        this.memberCount = members != null ? members.size() : 1;
        this.role = "member";
        this.ownerId = ownerId;
    }

    // 🔴 新增的建構子（支援 memberCount 和 role）
    public GroupItem(String id, String name, int memberCount, String role, String ownerId) {
        this.id = id;
        this.name = name;
        this.memberCount = memberCount;
        this.role = role != null ? role : "member";
        this.ownerId = ownerId;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public String getRole() {
        return role;
    }

    public String getOwnerId() {
        return ownerId;
    }

    // 為了相容舊程式碼，保留這個方法
    public List<String> getMembers() {
        return null;  // 不再使用
    }
}