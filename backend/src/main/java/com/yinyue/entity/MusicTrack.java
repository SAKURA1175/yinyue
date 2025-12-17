package com.yinyue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ======================================================================================
 * 类名：MusicTrack (音乐档案卡)
 * 
 * 作用：这个类对应数据库里的一张表，叫 "music_track"。
 * 
 * 想象一下，数据库是一个大仓库，"music_track" 是仓库里的一个货架。
 * 而这个类（MusicTrack）就是一张标准的“货物登记卡”。
 * 每当我们往货架上放一首歌，就要填一张这样的卡片，记录这首歌的名字、谁唱的、文件放在哪等等。
 * ======================================================================================
 */
@Entity // 告诉 JPA（数据库管家）：这是一个需要存进数据库的实体类。
@Table(name = "music_track") // 告诉管家：请把它存在名为 "music_track" 的那张表里。
public class MusicTrack {

    // @Id: 这是一个身份证号，每条记录必须独一无二。
    // @GeneratedValue: 身份证号由数据库自动生成（1, 2, 3, 4...），我们不用操心。
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==========================================
    // 第一部分：音乐基本信息
    // ==========================================
    
    // @Column(nullable = false): 这一栏必须填，不能为空。一首歌怎么能没有名字呢？
    @Column(nullable = false)
    private String title; // 歌名

    @Column
    private String artist; // 歌手

    @Column
    private String album; // 专辑名

    // ==========================================
    // 第二部分：文件存放信息
    // ==========================================

    @Column(nullable = false)
    private String filePath; // 文件路径：记录mp3文件在电脑硬盘上的具体位置（比如 d:/uploads/music/1.mp3）

    @Column
    private Long fileSize; // 文件大小：这首歌占了多少字节

    // ==========================================
    // 第三部分：AI 分析成果
    // ==========================================

    // LONGTEXT: 这是一个很大的文本框，能存很多字。因为 AI 分析的结果可能很长。
    @Column(columnDefinition = "LONGTEXT")
    private String auddResult; // Audd 听歌识曲的原始结果（JSON格式）

    @Column(columnDefinition = "LONGTEXT")
    private String aiAnalysis; // 阿里云通义千问分析出来的情感、意境描述（JSON格式）

    // LONGBLOB: 二进制大对象。这里直接把图片的二进制数据存在数据库里。
    // 虽然通常建议只存路径，但这里为了方便演示，直接存了图片本身。
    @Column(columnDefinition = "LONGBLOB")
    private byte[] albumCover; // AI 画出来的专辑封面图片数据

    @Column
    private String coverUrl; // 封面的访问链接（如果图片存在文件系统里，就用这个）

    // ==========================================
    // 第四部分：管理信息
    // ==========================================

    @Column
    private LocalDateTime createdAt; // 创建时间：这首歌是什么时候上传的

    @Column
    private LocalDateTime updatedAt; // 更新时间：最后一次修改信息是什么时候

    @Column
    private String status; // 状态：比如 "PENDING" (处理中), "COMPLETED" (完成), "FAILED" (失败)

    // ==========================================
    // Getters and Setters (存取方法)
    // 就像是自动取款机，想存钱用 Set，想取钱用 Get。
    // ==========================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getAuddResult() { return auddResult; }
    public void setAuddResult(String auddResult) { this.auddResult = auddResult; }

    public String getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; }

    public byte[] getAlbumCover() { return albumCover; }
    public void setAlbumCover(byte[] albumCover) { this.albumCover = albumCover; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
