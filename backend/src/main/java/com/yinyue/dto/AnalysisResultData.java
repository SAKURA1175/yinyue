package com.yinyue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnalysisResultData {

    private String theme;
    private String mood;

    @JsonProperty("visual_style")
    private String visualStyle;

    private String[] colors;

    private String summary;

    @JsonProperty("cover_concept")
    private String coverConcept;

    @JsonProperty("design_rationale")
    private String designRationale;

    @JsonProperty("prompt_notes")
    private String[] promptNotes;

    @JsonProperty("image_prompt_en")
    private String imagePromptEn;

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getVisualStyle() {
        return visualStyle;
    }

    public void setVisualStyle(String visualStyle) {
        this.visualStyle = visualStyle;
    }

    public String[] getColors() {
        return colors;
    }

    public void setColors(String[] colors) {
        this.colors = colors;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCoverConcept() {
        return coverConcept;
    }

    public void setCoverConcept(String coverConcept) {
        this.coverConcept = coverConcept;
    }

    public String getDesignRationale() {
        return designRationale;
    }

    public void setDesignRationale(String designRationale) {
        this.designRationale = designRationale;
    }

    public String[] getPromptNotes() {
        return promptNotes;
    }

    public void setPromptNotes(String[] promptNotes) {
        this.promptNotes = promptNotes;
    }

    public String getImagePromptEn() {
        return imagePromptEn;
    }

    public void setImagePromptEn(String imagePromptEn) {
        this.imagePromptEn = imagePromptEn;
    }
}
