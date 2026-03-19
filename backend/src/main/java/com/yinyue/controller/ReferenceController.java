package com.yinyue.controller;

import com.yinyue.dto.ReferenceSearchRequest;
import com.yinyue.service.ReferenceSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/reference")
public class ReferenceController {

    private final ReferenceSearchService referenceSearchService;

    public ReferenceController(ReferenceSearchService referenceSearchService) {
        this.referenceSearchService = referenceSearchService;
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchReferences(@RequestBody ReferenceSearchRequest request) {
        try {
            Map<String, Object> data = referenceSearchService.search(request);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "ok");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "参考检索失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
