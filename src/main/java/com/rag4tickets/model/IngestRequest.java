package com.rag4tickets.model;

public class IngestRequest {
    private String id;
    private String type; // "TICKET" or "PR"
    private String title;
    private String content;

    public IngestRequest() {}

    public IngestRequest(String id, String type, String title, String content) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
