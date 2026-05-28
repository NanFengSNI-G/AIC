package com.project.demo.controller;

import com.project.demo.dto.ApiResponse;
import com.project.demo.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final FileStorageService fileStorageService;

    /**
     * POST /api/images/upload
     * 上传图片，返回访问链接
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ApiResponse.error(400, "文件为空");
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ApiResponse.error(400, "仅支持图片文件");
            }

            String url = fileStorageService.saveImage(file);
            return ApiResponse.success(Map.of("url", url));
        } catch (Exception e) {
            return ApiResponse.error(500, "上传失败: " + e.getMessage());
        }
    }

    /**
     * GET /api/images/{fileName}
     * 根据文件名返回图片
     */
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
        try {
            Path path = fileStorageService.getImagePath(fileName);
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // 根据扩展名设置 Content-Type
            String contentType = "image/jpeg";
            String name = fileName.toLowerCase();
            if (name.endsWith(".png")) contentType = "image/png";
            else if (name.endsWith(".gif")) contentType = "image/gif";
            else if (name.endsWith(".webp")) contentType = "image/webp";
            else if (name.endsWith(".svg")) contentType = "image/svg+xml";

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
