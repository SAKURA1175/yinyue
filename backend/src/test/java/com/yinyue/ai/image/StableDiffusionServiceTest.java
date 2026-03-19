package com.yinyue.ai.image;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StableDiffusionServiceTest {

    @Test
    void testGenerateImage_ReturnsPlaceholderWhenServiceUnavailable() throws Exception {
        StableDiffusionService stableDiffusionService = createService(null, false);

        StableDiffusionService.ImageGenerationOptions options = StableDiffusionService.ImageGenerationOptions.builder()
                .prompt("test prompt")
                .width(512)
                .height(512)
                .steps(20)
                .build();

        String result = stableDiffusionService.generateImage(options);
        BufferedImage image = decodeImage(result);

        assertNotNull(result);
        assertEquals(512, image.getWidth());
        assertEquals(512, image.getHeight());
    }

    @Test
    void testGenerateImage_Success() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("images", Collections.singletonList("base64imageString"));

        StableDiffusionService stableDiffusionService = createService(mockResponse, true);

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
    void testGenerateImage_ApiReturnsNullFallsBackToPlaceholder() throws Exception {
        StableDiffusionService stableDiffusionService = createService(null, true);

        StableDiffusionService.ImageGenerationOptions options = StableDiffusionService.ImageGenerationOptions.builder()
                .prompt("test prompt")
                .build();

        String result = stableDiffusionService.generateImage(options);
        BufferedImage image = decodeImage(result);

        assertNotNull(result);
        assertEquals(512, image.getWidth());
        assertEquals(512, image.getHeight());
    }

    @Test
    void testGenerateImage_ApiReturnsNoImagesFallsBackToPlaceholder() throws Exception {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("images", Collections.emptyList());

        StableDiffusionService stableDiffusionService = createService(mockResponse, true);

        StableDiffusionService.ImageGenerationOptions options = StableDiffusionService.ImageGenerationOptions.builder()
                .prompt("test prompt")
                .build();

        String result = stableDiffusionService.generateImage(options);
        BufferedImage image = decodeImage(result);

        assertNotNull(result);
        assertEquals(512, image.getWidth());
        assertEquals(512, image.getHeight());
    }

    private StableDiffusionService createService(Map<String, Object> response, boolean serviceAvailable) {
        StubRestTemplate restTemplate = new StubRestTemplate();
        restTemplate.setResponse(response);
        restTemplate.setServiceAvailable(serviceAvailable);

        StableDiffusionService stableDiffusionService = new StableDiffusionService(restTemplate);
        ReflectionTestUtils.setField(stableDiffusionService, "sdEndpoint", "http://127.0.0.1:7860");
        ReflectionTestUtils.setField(stableDiffusionService, "sdEnabled", true);
        return stableDiffusionService;
    }

    private BufferedImage decodeImage(String base64) throws Exception {
        byte[] bytes = java.util.Base64.getDecoder().decode(base64);
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    private static class StubRestTemplate extends RestTemplate {
        private Map<String, Object> response;
        private boolean serviceAvailable;

        void setResponse(Map<String, Object> response) {
            this.response = response;
        }

        void setServiceAvailable(boolean serviceAvailable) {
            this.serviceAvailable = serviceAvailable;
        }

        @Override
        public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) {
            if (serviceAvailable) {
                return responseType.cast(Map.of("status", "ok"));
            }
            throw new RuntimeException("service unavailable");
        }

        @Override
        public <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables) {
            if (request != null) {
                assertTrue(request instanceof HttpEntity);
            }
            return responseType.cast(response);
        }
    }
}
