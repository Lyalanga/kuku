package com.example.fowltyphoidmonitor.data.models;

public class DiseaseInfo {
    private String category;
    private String content;

    // No-args constructor
    public DiseaseInfo() {}

    // Parameterized constructor
    public DiseaseInfo(String category, String content) {
        this.category = category;
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}