package com.yinyue.controller;

import com.yinyue.dto.UploadResponseData;
import com.yinyue.entity.MusicTrack;
import com.yinyue.service.FileUploadService;
import com.yinyue.service.MusicTrackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final MusicTrackService musicTrackService;

    public FileUploadController(FileUploadService fileUploadService, MusicTrackService musicTrackService) {
        this.fileUploadService = fileUploadService;
        this.musicTrackService = musicTrackService;
    }

    /**
     * 上传音频文件
     */
    @PostMapping("/audio")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            String filePath = fileUploadService.uploadFile(file, "audio");
            MusicTrack track = musicTrackService.createUploadRecord(file.getOriginalFilename(), filePath, file.getSize());

            UploadResponseData data = new UploadResponseData();
            data.setUploadId(track.getId());
            data.setFileName(file.getOriginalFilename());
            data.setFileSize(file.getSize());
            data.setStatus(track.getStatus());

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "文件上传成功");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", "文件上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "文件上传失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 上传图片文件
     */
    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            fileUploadService.uploadFile(file, "image");

            UploadResponseData data = new UploadResponseData();
            data.setFileName(file.getOriginalFilename());
            data.setFileSize(file.getSize());
            data.setStatus("UPLOADED");

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "图片上传成功");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", "图片上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "图片上传失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
