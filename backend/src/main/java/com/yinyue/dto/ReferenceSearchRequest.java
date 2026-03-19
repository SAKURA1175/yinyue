package com.yinyue.dto;

import java.util.Map;

public class ReferenceSearchRequest {

    private Long uploadId;
    private String intentText;
    private Map<String, Object> audioProfile;
    private Map<String, Object> semanticProfile;
    private Integer topK;
    private Integer topN;

    public Long getUploadId() {
        return uploadId;
    }

    public void setUploadId(Long uploadId) {
        this.uploadId = uploadId;
    }

    public String getIntentText() {
        return intentText;
    }

    public void setIntentText(String intentText) {
        this.intentText = intentText;
    }

    public Map<String, Object> getAudioProfile() {
        return audioProfile;
    }

    public void setAudioProfile(Map<String, Object> audioProfile) {
        this.audioProfile = audioProfile;
    }

    public Map<String, Object> getSemanticProfile() {
        return semanticProfile;
    }

    public void setSemanticProfile(Map<String, Object> semanticProfile) {
        this.semanticProfile = semanticProfile;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Integer getTopN() {
        return topN;
    }

    public void setTopN(Integer topN) {
        this.topN = topN;
    }
}
