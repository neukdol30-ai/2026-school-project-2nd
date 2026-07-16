package com.siyan1234.itproject2nd.board.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/board/image")
public class BoardImageController {

    @Value("${file.board-image-dir}")
    private String boardImageDir;

    private static final long MAX_IMAGE_SIZE =
            10L * 1024L * 1024L; // 10MB

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );


    // TOAST UI 본문 이미지 업로드
    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("image") MultipartFile image
    ) {

        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "이미지를 선택해주세요."));
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "이미지는 10MB 이하만 가능합니다."));
        }

        String contentType = image.getContentType();

        if (contentType == null
                || !ALLOWED_IMAGE_TYPES.contains(contentType)) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "허용되지 않는 이미지 형식입니다."));
        }

        String originalName = image.getOriginalFilename();

        // 원본 파일명에서 확장자 추출
        String extension = getExtension(originalName);

        // 파일 이름 중복 방지를 위해 UUID 사용
        String savedName = UUID.randomUUID() + extension;

        Path uploadPath = Paths.get(boardImageDir)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(uploadPath);

            Path targetPath = uploadPath
                    .resolve(savedName)
                    .normalize();

            // 상위 경로 접근 방지
            if (!targetPath.startsWith(uploadPath)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "잘못된 파일 경로입니다."));
            }

            Files.copy(
                    image.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "이미지 저장에 실패했습니다."));
        }

        // 브라우저에서 접근할 이미지 주소
        String imageUrl =
                "/uploads/board-images/" + savedName;

        return ResponseEntity.ok(
                Map.of("imageUrl", imageUrl)
        );
    }

    private String getExtension(String filename) {

        if (filename == null) {
            return "";
        }

        int index = filename.lastIndexOf(".");

        if (index < 0) {
            return "";
        }

        return filename.substring(index).toLowerCase();
    }
}
