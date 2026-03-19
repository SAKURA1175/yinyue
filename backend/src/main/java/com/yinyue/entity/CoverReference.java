package com.yinyue.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cover_reference")
public class CoverReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    @Column
    private Integer releaseYear;

    @Column
    private String genre;

    @Column(columnDefinition = "JSON")
    private String moodTags;

    @Column(columnDefinition = "JSON")
    private String styleTags;

    @Column(columnDefinition = "JSON")
    private String colorPalette;

    @Column(columnDefinition = "JSON")
    private String compositionTags;

    @Column
    private String sourceUrl;

    @Column(nullable = false)
    private String imageStorageKey;

    @Column(nullable = false)
    private String licenseTag = "unknown";

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getMoodTags() {
        return moodTags;
    }

    public void setMoodTags(String moodTags) {
        this.moodTags = moodTags;
    }

    public String getStyleTags() {
        return styleTags;
    }

    public void setStyleTags(String styleTags) {
        this.styleTags = styleTags;
    }

    public String getColorPalette() {
        return colorPalette;
    }

    public void setColorPalette(String colorPalette) {
        this.colorPalette = colorPalette;
    }

    public String getCompositionTags() {
        return compositionTags;
    }

    public void setCompositionTags(String compositionTags) {
        this.compositionTags = compositionTags;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getImageStorageKey() {
        return imageStorageKey;
    }

    public void setImageStorageKey(String imageStorageKey) {
        this.imageStorageKey = imageStorageKey;
    }

    public String getLicenseTag() {
        return licenseTag;
    }

    public void setLicenseTag(String licenseTag) {
        this.licenseTag = licenseTag;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
