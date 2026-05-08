package com.trip;

import com.trip.common.ErrorCode;
import com.trip.exception.BusinessException;
import com.trip.service.AuthService;
import com.trip.service.impl.FileServiceImpl;
import com.trip.vo.response.FileUploadResultVO;
import com.trip.vo.response.UserVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileServiceTests {

    private final Path uploadRoot = Path.of("target/test-uploads-unit").toAbsolutePath().normalize();
    private final FileServiceImpl fileService = new FileServiceImpl(
            new StubAuthService(),
            uploadRoot.toString(),
            "/files",
            DataSize.ofMegabytes(1));

    @AfterEach
    void cleanUploadedFiles() throws IOException {
        if (!Files.exists(uploadRoot)) {
            return;
        }
        try (var paths = Files.walk(uploadRoot)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
        }
    }

    @Test
    void uploadShouldSaveAllowedImageAndReturnFileUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "fake-image-content".getBytes());

        FileUploadResultVO result = fileService.upload(file, "diary", null, "Bearer token");

        assertEquals("diary", result.getBizType());
        assertTrue(result.getFileName().endsWith(".jpg"));
        assertTrue(result.getFileUrl().startsWith("/files/diary/"));
        assertTrue(Files.exists(uploadRoot.resolve(result.getFileUrl().substring("/files/".length()))));
    }

    @Test
    void uploadShouldRejectInvalidBizType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "fake-image-content".getBytes());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fileService.upload(file, "unknown", null, "Bearer token"));

        assertEquals(ErrorCode.COMMON_002, exception.getErrorCode());
    }

    @Test
    void uploadShouldRejectUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "plain-text".getBytes());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fileService.upload(file, "diary", null, "Bearer token"));

        assertEquals(ErrorCode.FILE_002, exception.getErrorCode());
    }

    @Test
    void uploadShouldRejectTooLargeFile() {
        byte[] content = new byte[(int) DataSize.ofMegabytes(2).toBytes()];
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", content);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fileService.upload(file, "diary", null, "Bearer token"));

        assertEquals(ErrorCode.FILE_003, exception.getErrorCode());
    }

    private static class StubAuthService implements AuthService {

        @Override
        public com.trip.vo.response.RegisterResponse register(com.trip.dto.request.RegisterRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.trip.vo.response.LoginResponse login(com.trip.dto.request.LoginRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserVO getCurrentUser(String authorizationHeader) {
            UserVO user = new UserVO();
            user.setId(1L);
            user.setUsername("tester");
            user.setRole("user");
            return user;
        }
    }
}
