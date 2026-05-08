package com.trip.service;

import com.trip.vo.response.FileUploadResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    FileUploadResultVO upload(MultipartFile file, String bizType, Long refId, String authorizationHeader);
}
