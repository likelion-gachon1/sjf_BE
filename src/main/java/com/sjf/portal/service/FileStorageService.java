package com.sjf.portal.service;

import com.sjf.portal.exception.PortalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class FileStorageService {

    private static final long MAX_IMAGE_SIZE = 10L * 1024L * 1024L;

    private final Path storageRoot;

    public FileStorageService(
            @Value("${portal.storage-path:./uploads}") String storagePath
    ) {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();

        try {
            Files.createDirectories(storageRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("업로드 폴더를 만들 수 없습니다.", exception);
        }
    }

    public String saveJpeg(String sessionId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new PortalApiException(HttpStatus.BAD_REQUEST, "촬영 이미지가 필요합니다.");
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new PortalApiException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "이미지는 최대 10MB까지 업로드할 수 있습니다."
            );
        }

        String contentType = image.getContentType();
        if (contentType == null ||
                !(contentType.equalsIgnoreCase("image/jpeg") ||
                        contentType.equalsIgnoreCase("image/jpg"))) {
            throw new PortalApiException(HttpStatus.BAD_REQUEST, "JPEG 이미지만 업로드할 수 있습니다.");
        }

        String filename = sessionId + ".jpg";
        Path target = resolveSafePath(filename);

        try {
            byte[] bytes = image.getBytes();
            if (bytes.length < 2 ||
                    (bytes[0] & 0xFF) != 0xFF ||
                    (bytes[1] & 0xFF) != 0xD8) {
                throw new PortalApiException(HttpStatus.BAD_REQUEST, "올바른 JPEG 파일이 아닙니다.");
            }

            Files.write(
                    target,
                    bytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            return filename;
        } catch (IOException exception) {
            throw new PortalApiException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장에 실패했습니다.");
        }
    }

    public Resource load(String filename) {
        Path target = resolveSafePath(filename);

        try {
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new PortalApiException(HttpStatus.NOT_FOUND, "촬영 이미지를 찾을 수 없습니다.");
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new PortalApiException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 경로가 올바르지 않습니다.");
        }
    }

    public void deleteQuietly(String filename) {
        try {
            Files.deleteIfExists(resolveSafePath(filename));
        } catch (IOException ignored) {
        }
    }

    private Path resolveSafePath(String filename) {
        Path target = storageRoot.resolve(filename).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new PortalApiException(HttpStatus.BAD_REQUEST, "잘못된 파일 경로입니다.");
        }
        return target;
    }
}
