package com.trip.integration.ai;

import com.trip.config.AiAnimationProperties;
import com.trip.model.ai.AnimationMediaInput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 将已落库的本地日记图片安全转换为多模态请求所需的 Base64。
 */
@Component
public class LocalAnimationImageLoader {

    private static final String DIARY_PATH_PREFIX = "diary/";

    private final Path uploadRoot;
    private final String accessPrefix;
    private final AiAnimationProperties properties;

    public LocalAnimationImageLoader(
            @Value("${trip.file.upload-dir:uploads}") String uploadDir,
            @Value("${trip.file.access-prefix:/files}") String accessPrefix,
            AiAnimationProperties properties) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.accessPrefix = normalizeAccessPrefix(accessPrefix);
        this.properties = properties;
    }

    public List<LoadedAnimationImage> load(List<AnimationMediaInput> mediaList) {
        if (mediaList == null
                || mediaList.isEmpty()
                || mediaList.size() > properties.getMaxImages()) {
            throw new AIProviderException("IMAGE_COUNT_INVALID");
        }

        long maxSingleSize = properties.getMaxImageSize().toBytes();
        long maxTotalSize = properties.getMaxTotalImageSize().toBytes();
        long totalSize = 0;
        List<LoadedAnimationImage> images = new ArrayList<>(mediaList.size());
        for (AnimationMediaInput media : mediaList) {
            Path imagePath = resolveDiaryImage(media);
            try {
                long imageSize = Files.size(imagePath);
                if (imageSize <= 0 || imageSize > maxSingleSize) {
                    throw new AIProviderException("IMAGE_SIZE_INVALID");
                }
                totalSize += imageSize;
                if (totalSize > maxTotalSize) {
                    throw new AIProviderException("TOTAL_IMAGE_SIZE_INVALID");
                }

                byte[] bytes = Files.readAllBytes(imagePath);
                String mimeType = detectMimeType(bytes);
                images.add(new LoadedAnimationImage(
                        media.mediaId(),
                        media.fileUrl(),
                        mimeType,
                        Base64.getEncoder().encodeToString(bytes)));
            } catch (IOException exception) {
                throw new AIProviderException("IMAGE_READ_FAILED", exception);
            }
        }
        return List.copyOf(images);
    }

    private Path resolveDiaryImage(AnimationMediaInput media) {
        if (media == null || media.mediaId() == null || !StringUtils.hasText(media.fileUrl())) {
            throw new AIProviderException("IMAGE_REFERENCE_INVALID");
        }
        String requiredPrefix = accessPrefix + "/" + DIARY_PATH_PREFIX;
        if (!media.fileUrl().startsWith(requiredPrefix)) {
            throw new AIProviderException("IMAGE_REFERENCE_INVALID");
        }

        String relativePath = media.fileUrl().substring((accessPrefix + "/").length());
        Path candidate = uploadRoot.resolve(relativePath).normalize();
        if (!candidate.startsWith(uploadRoot) || !Files.isRegularFile(candidate)) {
            throw new AIProviderException("IMAGE_NOT_FOUND");
        }
        Path current = uploadRoot;
        for (Path segment : uploadRoot.relativize(candidate)) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) {
                throw new AIProviderException("IMAGE_PATH_INVALID");
            }
        }
        return candidate;
    }

    private String detectMimeType(byte[] bytes) {
        if (isJpeg(bytes)) {
            return "image/jpeg";
        }
        if (isPng(bytes)) {
            return "image/png";
        }
        if (isWebp(bytes)) {
            return "image/webp";
        }
        throw new AIProviderException("IMAGE_FORMAT_UNSUPPORTED");
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        int[] signature = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if ((bytes[index] & 0xFF) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P';
    }

    private String normalizeAccessPrefix(String prefix) {
        String normalized = StringUtils.hasText(prefix) ? prefix.trim() : "/files";
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
