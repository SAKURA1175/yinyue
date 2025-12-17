package com.yinyue.controller;

import com.yinyue.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 上传音频文件
     */
    @PostMapping("/audio")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            String filePath = fileUploadService.uploadFile(file, "audio");
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "文件上传成功");
            response.put("data", Map.of(
                    "filePath", filePath,
                    "fileName", file.getOriginalFilename(),
                    "fileSize", file.getSize()
            ));
            
            return ResponseEntity.ok(response);
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
            String filePath = fileUploadService.uploadFile(file, "image");
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "图片上传成功");
            response.put("data", Map.of(
                    "filePath", filePath,
                    "fileName", file.getOriginalFilename(),
                    "fileSize", file.getSize()
            ));
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "图片上传失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
