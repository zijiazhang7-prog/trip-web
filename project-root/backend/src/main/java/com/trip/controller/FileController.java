package com.trip.controller;

import com.trip.common.ApiResponse;
import com.trip.service.FileService;
import com.trip.vo.response.FileUploadResultVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口。
 */
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ApiResponse<FileUploadResultVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bizType") String bizType,
            @RequestParam(value = "refId", required = false) Long refId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return ApiResponse.success(fileService.upload(file, bizType, refId, authorizationHeader));
    }
}
