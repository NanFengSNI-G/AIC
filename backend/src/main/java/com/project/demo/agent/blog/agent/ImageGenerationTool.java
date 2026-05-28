package com.project.demo.agent.blog.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.demo.service.FileStorageService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.UUID;

/**
 * 图片生成工具 — 调用 DashScope Qwen-Image-2.0-Pro 模型，将生成的图片保存到本地。
 */
@Slf4j
@Component
public class ImageGenerationTool {

    private static final String DASHSCOPE_API_URL =
        "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";
    private static final String MODEL = "qwen-image-2.0-pro";

    private final String apiKey;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ImageGenerationTool(
            @Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey,
            FileStorageService fileStorageService,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    /**
     * 根据提示词生成图片，保存到本地，返回访问链接。
     */
    @Tool("根据提示词生成配图。输入详细的图片描述（包括内容、风格、色调、构图等），" +
          "返回图片的本地访问链接。每张需要的配图调用一次。")
    public String generateImage(
            @P("详细的文生图提示词，描述图片内容、风格、色调、构图等")
            String prompt) {

        log.info("[ImageTool] 开始生成图片, prompt={}", prompt.substring(0, Math.min(100, prompt.length())));

        try {
            // 1. 构建请求体（对齐官方 multimodal-generation API 格式）
            var textNode = objectMapper.createObjectNode().put("text", prompt);
            var contentArray = objectMapper.createArrayNode().add(textNode);
            var userMessage = objectMapper.createObjectNode()
                .put("role", "user");
            userMessage.set("content", contentArray);
            var messages = objectMapper.createArrayNode().add(userMessage);

            var input = objectMapper.createObjectNode();
            input.set("messages", messages);

            var params = objectMapper.createObjectNode()
                .put("size", "1024*1024")
                .put("negative_prompt", "低分辨率，低画质，肢体畸形，手指畸形，画面过饱和，蜡像感，人脸无细节，过度光滑，画面具有AI感。构图混乱。文字模糊，扭曲。")
                .put("prompt_extend", false)
                .put("watermark", false);

            var body = objectMapper.createObjectNode()
                .put("model", MODEL);
            body.set("input", input);
            body.set("parameters", params);
            String requestBody = objectMapper.writeValueAsString(body);

            // 2. 调用 DashScope API（带限流重试）
            String responseBody = null;
            int maxRetries = 3;
            for (int attempt = 0; attempt < maxRetries; attempt++) {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DASHSCOPE_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(180))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    responseBody = response.body();
                    break;
                }

                // 检查是否为限流错误
                String errorCode = "";
                try {
                    JsonNode err = objectMapper.readTree(response.body());
                    errorCode = err.path("code").asText("");
                } catch (Exception ignored) {}

                if (response.statusCode() == 429 || errorCode.contains("Throttling") || errorCode.contains("RateQuota")) {
                    if (attempt < maxRetries - 1) {
                        long waitMs = (long) Math.pow(2, attempt + 1) * 1000;  // 2s, 4s, 8s
                        log.warn("[ImageTool] 限流，{} 秒后重试 (attempt {}/{})", waitMs / 1000, attempt + 1, maxRetries);
                        try { Thread.sleep(waitMs); } catch (InterruptedException ignored) {}
                        continue;
                    }
                }

                log.error("[ImageTool] API 返回错误: status={}, body={}", response.statusCode(), response.body());
                try {
                    JsonNode err = objectMapper.readTree(response.body());
                    String code = err.path("code").asText("");
                    String msg = err.path("message").asText("");
                    return "图片生成失败: " + (code.isBlank() ? "" : code + " - ") + (msg.isBlank() ? "HTTP " + response.statusCode() : msg);
                } catch (Exception ignored) {
                    return "图片生成失败: HTTP " + response.statusCode();
                }
            }

            if (responseBody == null) {
                return "图片生成失败: 重试" + maxRetries + "次后仍然失败";
            }

            // 3. 解析响应，获取图片 URL
            // multimodal-generation 响应格式: output.choices[0].message.content[0].image
            JsonNode root = objectMapper.readTree(responseBody);
            String imageUrl = null;

            // 尝试新格式: choices[0].message.content[0].image
            JsonNode choices = root.path("output").path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode content = choices.get(0).path("message").path("content");
                if (content.isArray() && !content.isEmpty()) {
                    imageUrl = content.get(0).path("image").asText();
                }
            }

            // 兼容旧格式: results[0].url
            if (imageUrl == null || imageUrl.isBlank()) {
                JsonNode results = root.path("output").path("results");
                if (results.isArray() && !results.isEmpty()) {
                    imageUrl = results.get(0).path("url").asText();
                }
            }

            if (imageUrl == null || imageUrl.isBlank()) {
                log.error("[ImageTool] 响应中无图片 URL: {}", responseBody);
                return "图片生成失败: 模型未返回图片";
            }

            log.info("[ImageTool] 获取到 DashScope 图片链接: {}", imageUrl);

            // 4. 下载图片到本地
            HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

            HttpResponse<InputStream> downloadResponse = httpClient.send(downloadRequest,
                HttpResponse.BodyHandlers.ofInputStream());

            if (downloadResponse.statusCode() != 200) {
                log.error("[ImageTool] 下载图片失败: status={}", downloadResponse.statusCode());
                return "图片下载失败: HTTP " + downloadResponse.statusCode();
            }

            // 5. 保存到本地
            String fileName = UUID.randomUUID() + ".png";
            Path tmpFile = Files.createTempFile("img-", ".png");
            try (InputStream is = downloadResponse.body()) {
                Files.copy(is, tmpFile, StandardCopyOption.REPLACE_EXISTING);
            }

            Path targetPath = Path.of("uploads", "images", fileName).toAbsolutePath().normalize();
            Files.createDirectories(targetPath.getParent());
            Files.move(tmpFile, targetPath, StandardCopyOption.REPLACE_EXISTING);

            String localUrl = "/api/images/" + fileName;
            log.info("[ImageTool] 图片已保存: {}", localUrl);
            return localUrl;

        } catch (Exception e) {
            log.error("[ImageTool] 图片生成异常", e);
            return "图片生成失败: " + e.getMessage();
        }
    }
}
