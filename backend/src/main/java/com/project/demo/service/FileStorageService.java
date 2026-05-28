package com.project.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath, "images").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
            log.info("图片存储目录: {}", this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录: " + this.uploadDir, e);
        }
    }

    /**
     * 保存图片文件，返回访问路径。
     * 文件名格式: {uuid}.{ext}
     */
    public String saveImage(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        String fileName = UUID.randomUUID().toString() + ext;

        Path targetPath = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("图片已保存: {} -> {}", originalName, targetPath);
        return "/api/images/" + fileName;
    }

    /**
     * 根据文件名读取图片文件。
     */
    public Path getImagePath(String fileName) {
        Path path = uploadDir.resolve(fileName).normalize();
        // 防止路径穿越攻击
        if (!path.startsWith(uploadDir)) {
            throw new SecurityException("非法的文件路径: " + fileName);
        }
        return path;
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }
}
