package com.yinyue.service;

import com.yinyue.ai.image.StableDiffusionService;
import com.yinyue.entity.MusicTrack;
import com.yinyue.repository.MusicTrackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * ======================================================================================
 * 类名：MusicTrackService (音乐业务经理)
 * 
 * 作用：它是整个音乐模块的“大管家”。
 * 它负责协调仓库管理员 (Repository) 和其他服务 (比如 AI 画图) 协同工作。
 * 
 * Controller 只管接收命令，具体的活儿（比如保存数据、更新状态、转换图片格式）都是这个经理来干。
 * ======================================================================================
 */
@Service
public class MusicTrackService {

    private static final Logger log = LoggerFactory.getLogger(MusicTrackService.class);

    // 仓库管理员：负责存取数据
    private final MusicTrackRepository musicTrackRepository;
    
    // AI 画家：负责画图
    private final StableDiffusionService stableDiffusionService;

    // 构造函数：招聘员工入职
    public MusicTrackService(MusicTrackRepository musicTrackRepository, StableDiffusionService stableDiffusionService) {
        this.musicTrackRepository = musicTrackRepository;
        this.stableDiffusionService = stableDiffusionService;
    }

    /**
     * 方法名：saveGeneratedCover (保存 AI 生成的封面)
     * 
     * 作用：把 AI 画出来的图存到数据库里。
     * 
     * @param prompt 当时用的提示词（画这幅画的要求）
     * @param base64Image 图片的 Base64 编码（图片本身）
     * @return 保存好的音乐记录
     */
    public MusicTrack saveGeneratedCover(String prompt, String base64Image) {
        // 创建一张新的登记卡
        MusicTrack track = new MusicTrack();
        track.setTitle("AI Generated Track"); // 先起个默认名字
        track.setArtist("AI Composer"); // 歌手就是 AI
        track.setFilePath(""); // 因为这只是个封面，还没有音频文件
        track.setCreatedAt(LocalDateTime.now()); // 记录现在的时间
        track.setUpdatedAt(LocalDateTime.now());
        track.setStatus("COMPLETED"); // 状态设为完成
        track.setAiAnalysis("Prompt: " + prompt); // 把提示词记下来，方便以后看
        
        // 如果有图片数据
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                // 把 Base64 字符串（一串乱码）变回二进制数据（真正的图片）
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                track.setAlbumCover(imageBytes); // 存进登记卡
            } catch (IllegalArgumentException e) {
                // 如果图片数据坏了，记录错误日志，并把状态改成失败
                log.error("Base64 图片解码失败", e);
                track.setStatus("FAILED_IMAGE_DECODE");
            }
        }
        
        // 叫仓库管理员把这张卡存起来
        return musicTrackRepository.save(track);
    }

    /**
     * 方法名：getAllHistory (获取所有历史记录)
     * 
     * 作用：把仓库里所有的歌都拿出来。
     */
    public List<MusicTrack> getAllHistory() {
        return musicTrackRepository.findAll();
    }

    /**
     * 方法名：saveTrack (保存音乐)
     * 
     * 作用：通用的保存方法。如果没填时间，就自动填上当前时间。
     */
    public MusicTrack saveTrack(MusicTrack track) {
        if (track.getCreatedAt() == null) {
            track.setCreatedAt(LocalDateTime.now());
        }
        track.setUpdatedAt(LocalDateTime.now());
        return musicTrackRepository.save(track);
    }

    /**
     * 方法名：updateTrack (更新音乐信息)
     * 
     * 作用：修改一首歌的信息。
     * 
     * @param id 要修改哪首歌（身份证号）
     * @param trackDetails 新的信息
     * @return 修改后的记录
     */
    public MusicTrack updateTrack(Long id, MusicTrack trackDetails) {
        // 先去仓库里找找这首歌在不在。如果找不到，就报错。
        MusicTrack track = musicTrackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到这首歌，ID: " + id));
        
        // 把新的信息填进去
        track.setTitle(trackDetails.getTitle());
        track.setArtist(trackDetails.getArtist());
        track.setAlbum(trackDetails.getAlbum());
        track.setFilePath(trackDetails.getFilePath());
        track.setAlbumCover(trackDetails.getAlbumCover());
        track.setAiAnalysis(trackDetails.getAiAnalysis());
        track.setStatus(trackDetails.getStatus());
        track.setUpdatedAt(LocalDateTime.now()); // 记得更新一下“修改时间”
        
        // 保存修改
        return musicTrackRepository.save(track);
    }

    /**
     * 方法名：getAllTracks (获取所有音乐)
     * 作用：跟 getAllHistory 一样，拿所有歌。
     */
    public List<MusicTrack> getAllTracks() {
        return musicTrackRepository.findAll();
    }

    /**
     * 方法名：getTrackById (根据 ID 找歌)
     * 
     * 作用：精准查找某某一首歌。
     */
    public MusicTrack getTrackById(Long id) {
        return musicTrackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到这首歌，ID: " + id));
    }
}
