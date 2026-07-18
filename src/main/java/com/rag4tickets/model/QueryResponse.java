package com.rag4tickets.model;

import java.io.Serializable;
import java.util.List;

public class QueryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String resolution;
    private List<String> references;
    private long latencyMs;
    private String source;
    private double similarityScore;
    private double groundingScore;

    public QueryResponse() {}

    public QueryResponse(String resolution, List<String> references, long latencyMs, String source, double similarityScore, double groundingScore) {
        this.resolution = resolution;
        this.references = references;
        this.latencyMs = latencyMs;
        this.source = source;
        this.similarityScore = similarityScore;
        this.groundingScore = groundingScore;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public double getGroundingScore() {
        return groundingScore;
    }

    public void setGroundingScore(double groundingScore) {
        this.groundingScore = groundingScore;
    }
}
