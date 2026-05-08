package com.trip.service.impl;

import com.trip.common.ErrorCode;
import com.trip.exception.BusinessException;
import com.trip.service.AuthService;
import com.trip.service.FileService;
import com.trip.vo.response.FileUploadResultVO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileServiceImpl implements FileService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Set<String> ALLOWED_BIZ_TYPES = Set.of("avatar", "diary", "destination");
    private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES = Map.of(
            ".jpg", Set.of("image/jpeg"),
            ".jpeg", Set.of("image/jpeg"),
            ".png", Set.of("image/png"),
            ".gif", Set.of("image/gif"),
            ".webp", Set.of("image/webp"),
            ".mp4", Set.of("video/mp4"));

    private final AuthService authService;
    private final Path uploadRoot;
    private final String accessPrefix;
    private final long maxSizeBytes;

    public FileServiceImpl(
            AuthService authService,
            @Value("${trip.file.upload-dir:uploads}") String uploadDir,
            @Value("${trip.file.access-prefix:/files}") String accessPrefix,
            @Value("${trip.file.max-size:5MB}") DataSize maxSize) {
        this.authService = authService;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.accessPrefix = normalizeAccessPrefix(accessPrefix);
        this.maxSizeBytes = maxSize.toBytes();
    }

    @Override
    public FileUploadResultVO upload(MultipartFile file, String bizType, Long refId, String authorizationHeader) {
        authService.getCurrentUser(authorizationHeader);

        String normalizedBizType = normalizeBizType(bizType);
        validateFile(file);

        String extension = getAllowedExtension(file);
        String datePath = LocalDate.now().format(DATE_FORMATTER);
        String storedFileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetDirectory = uploadRoot.resolve(normalizedBizType).resolve(datePath).normalize();
        Path targetFile = targetDirectory.resolve(storedFileName).normalize();

        if (!targetFile.startsWith(uploadRoot)) {
            throw new BusinessException(ErrorCode.FILE_005);
        }

        try {
            Files.createDirectories(targetDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_005);
        }

        String fileUrl = String.join("/", accessPrefix, normalizedBizType, datePath, storedFileName);
        return new FileUploadResultVO(normalizedBizType, storedFileName, fileUrl);
    }

    private String normalizeBizType(String bizType) {
        String normalizedBizType = bizType == null ? "" : bizType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_BIZ_TYPES.contains(normalizedBizType)) {
            throw new BusinessException(ErrorCode.COMMON_002);
        }
        return normalizedBizType;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_001);
        }
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException(ErrorCode.FILE_003);
        }
    }

    private String getAllowedExtension(MultipartFile file) {
        String originalFilename = StringUtils.getFilename(file.getOriginalFilename());
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException(ErrorCode.FILE_002);
        }

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new BusinessException(ErrorCode.FILE_002);
        }

        String extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
        Set<String> allowedContentTypes = ALLOWED_CONTENT_TYPES.get(extension);
        if (allowedContentTypes == null || !allowedContentTypes.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.FILE_002);
        }
        return extension;
    }

    private String normalizeAccessPrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "/files";
        }
        String normalizedPrefix = prefix.trim();
        if (!normalizedPrefix.startsWith("/")) {
            normalizedPrefix = "/" + normalizedPrefix;
        }
        if (normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }
        return normalizedPrefix;
    }
}
