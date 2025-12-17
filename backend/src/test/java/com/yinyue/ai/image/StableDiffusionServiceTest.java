package com.yinyue.ai.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class StableDiffusionServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private StableDiffusionService stableDiffusionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stableDiffusionService = new StableDiffusionService(restTemplate);
        
        // 通过反射设置 sdEndpoint
        try {
            java.lang.reflect.Field field = StableDiffusionService.class.getDeclaredField("sdEndpoint");
            field.setAccessible(true);
            field.set(stableDiffusionService, "http://127.0.0.1:7860");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set sdEndpoint field", e);
        }
    }

    @Test
    void testGenerateImage_Success() {
        // Mock response
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("images", Collections.singletonList("base64imageString"));

        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        StableDiffusionService.ImageGenerationOptions options = StableDiffusionService.ImageGenerationOptions.builder()
                .prompt("test prompt")
                .width(512)
                .height(512)
                .steps(20)
                .build();

        String result = stableDiffusionService.generateImage(options);
        assertEquals("base64imageString", result);
    }

    @Test
    void testGenerateImage_ApiReturnsNull() {
        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(null);

        StableDiffusionService.ImageGenerationOptions options = StableDiffusionService.ImageGenerationOptions.builder()
                .prompt("test prompt")
                .build();

        Exception exception = assertThrows(RuntimeException.class, () -> stableDiffusionService.generateImage(options));
        assertTrue(exception.getMessage().contains("API 响应为空"));
    }
    
    @Test
    void testGenerateImage_ApiReturnsNoImages() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("images", Collections.emptyList());

        when(restTemplate.postForObject(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        StableDiffusionService.ImageGenerationOptions options = StableDiffusionService.ImageGenerationOptions.builder()
                .prompt("test prompt")
                .build();

        Exception exception = assertThrows(RuntimeException.class, () -> stableDiffusionService.generateImage(options));
        assertTrue(exception.getMessage().contains("未获得图片数据"));
    }
}
