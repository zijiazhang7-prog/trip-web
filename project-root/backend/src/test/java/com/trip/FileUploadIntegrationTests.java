package com.trip;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "trip.file.upload-dir=target/test-uploads-web")
@AutoConfigureMockMvc
class FileUploadIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadShouldRejectMissingAuthorizationHeader() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "fake-image-content".getBytes());

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("bizType", "diary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_003"));
    }

    @Test
    void uploadShouldRejectInvalidToken() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "fake-image-content".getBytes());

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("bizType", "diary")
                        .header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }
}
