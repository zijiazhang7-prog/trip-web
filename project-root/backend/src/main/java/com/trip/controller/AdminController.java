package com.trip.controller;

import com.trip.common.ApiResponse;
import com.trip.dto.imports.ImportPreviewResult;
import com.trip.dto.imports.ImportRequest;
import com.trip.dto.imports.ImportResult;
import com.trip.dto.request.AdminDestinationRequest;
import com.trip.dto.request.AdminFacilityRequest;
import com.trip.dto.request.AdminFoodRequest;
import com.trip.dto.request.AdminMapEdgeRequest;
import com.trip.dto.request.AdminMapNodeRequest;
import com.trip.dto.request.AdminPageQuery;
import com.trip.dto.request.AdminPlaceRequest;
import com.trip.dto.request.AdminStatusRequest;
import com.trip.common.ErrorCode;
import com.trip.exception.BusinessException;
import com.trip.service.AdminService;
import com.trip.vo.response.AdminDiaryVO;
import com.trip.vo.response.AdminDestinationVO;
import com.trip.vo.response.AdminFacilityVO;
import com.trip.vo.response.AdminImportBatchVO;
import com.trip.vo.response.AdminImportFailureVO;
import com.trip.vo.response.AdminMapEdgeVO;
import com.trip.vo.response.AdminMapNodeVO;
import com.trip.vo.response.AdminPlaceVO;
import com.trip.vo.response.AdminUserVO;
import com.trip.vo.response.FoodVO;
import com.trip.vo.response.PageResultVO;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin 最小后台接口，先支持目的地、设施和美食基础维护。
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/destinations")
    public ApiResponse<PageResultVO<AdminDestinationVO>> listDestinations(
            @Valid @ModelAttribute AdminPageQuery query) {
        return ApiResponse.success(adminService.listDestinations(query));
    }

    @PostMapping("/destinations")
    public ApiResponse<AdminDestinationVO> createDestination(
            @Valid @RequestBody AdminDestinationRequest request) {
        return ApiResponse.success(adminService.createDestination(request));
    }

    @PutMapping("/destinations/{id}")
    public ApiResponse<AdminDestinationVO> updateDestination(
            @PathVariable Long id,
            @Valid @RequestBody AdminDestinationRequest request) {
        return ApiResponse.success(adminService.updateDestination(id, request));
    }

    @DeleteMapping("/destinations/{id}")
    public ApiResponse<Boolean> deleteDestination(@PathVariable Long id) {
        return ApiResponse.success(adminService.deleteDestination(id));
    }

    @GetMapping("/places")
    public ApiResponse<PageResultVO<AdminPlaceVO>> listPlaces(
            @Valid @ModelAttribute AdminPageQuery query) {
        return ApiResponse.success(adminService.listPlaces(query));
    }

    @PostMapping("/places")
    public ApiResponse<AdminPlaceVO> createPlace(@Valid @RequestBody AdminPlaceRequest request) {
        return ApiResponse.success(adminService.createPlace(request));
    }

    @PutMapping("/places/{id}")
    public ApiResponse<AdminPlaceVO> updatePlace(
            @PathVariable Long id,
            @Valid @RequestBody AdminPlaceRequest request) {
        return ApiResponse.success(adminService.updatePlace(id, request));
    }

    @DeleteMapping("/places/{id}")
    public ApiResponse<Boolean> deletePlace(@PathVariable Long id) {
        return ApiResponse.success(adminService.deletePlace(id));
    }

    @GetMapping("/facilities")
    public ApiResponse<PageResultVO<AdminFacilityVO>> listFacilities(
            @Valid @ModelAttribute AdminPageQuery query) {
        return ApiResponse.success(adminService.listFacilities(query));
    }

    @PostMapping("/facilities")
    public ApiResponse<AdminFacilityVO> createFacility(
            @Valid @RequestBody AdminFacilityRequest request) {
        return ApiResponse.success(adminService.createFacility(request));
    }

    @PutMapping("/facilities/{id}")
    public ApiResponse<AdminFacilityVO> updateFacility(
            @PathVariable Long id,
            @Valid @RequestBody AdminFacilityRequest request) {
        return ApiResponse.success(adminService.updateFacility(id, request));
    }

    @DeleteMapping("/facilities/{id}")
    public ApiResponse<Boolean> deleteFacility(@PathVariable Long id) {
        return ApiResponse.success(adminService.deleteFacility(id));
    }

    @GetMapping("/foods")
    public ApiResponse<PageResultVO<FoodVO>> listFoods(
            @Valid @ModelAttribute AdminPageQuery query) {
        return ApiResponse.success(adminService.listFoods(query));
    }

    @PostMapping("/foods")
    public ApiResponse<FoodVO> createFood(@Valid @RequestBody AdminFoodRequest request) {
        return ApiResponse.success(adminService.createFood(request));
    }

    @PutMapping("/foods/{id}")
    public ApiResponse<FoodVO> updateFood(
            @PathVariable Long id,
            @Valid @RequestBody AdminFoodRequest request) {
        return ApiResponse.success(adminService.updateFood(id, request));
    }

    @DeleteMapping("/foods/{id}")
    public ApiResponse<Boolean> deleteFood(@PathVariable Long id) {
        return ApiResponse.success(adminService.deleteFood(id));
    }

    @GetMapping("/map/nodes")
    public ApiResponse<PageResultVO<AdminMapNodeVO>> listMapNodes(
            @Valid @ModelAttribute AdminPageQuery query) {
        return ApiResponse.success(adminService.listMapNodes(query));
    }

    @PostMapping("/map/nodes")
    public ApiResponse<AdminMapNodeVO> createMapNode(
            @Valid @RequestBody AdminMapNodeRequest request) {
        return ApiResponse.success(adminService.createMapNode(request));
    }

    @PutMapping("/map/nodes/{id}")
    public ApiResponse<AdminMapNodeVO> updateMapNode(
            @PathVariable Long id,
            @Valid @RequestBody AdminMapNodeRequest request) {
        return ApiResponse.success(adminService.updateMapNode(id, request));
    }

    @DeleteMapping("/map/nodes/{id}")
    public ApiResponse<Boolean> deleteMapNode(@PathVariable Long id) {
        return ApiResponse.success(adminService.deleteMapNode(id));
    }

    @GetMapping("/map/edges")
    public ApiResponse<PageResultVO<AdminMapEdgeVO>> listMapEdges(
            @Valid @ModelAttribute AdminPageQuery query) {
        return ApiResponse.success(adminService.listMapEdges(query));
    }

    @PostMapping("/map/edges")
    public ApiResponse<AdminMapEdgeVO> createMapEdge(
            @Valid @RequestBody AdminMapEdgeRequest request) {
        return ApiResponse.success(adminService.createMapEdge(request));
    }

    @PutMapping("/map/edges/{id}")
    public ApiResponse<AdminMapEdgeVO> updateMapEdge(
            @PathVariable Long id,
            @Valid @RequestBody AdminMapEdgeRequest request) {
        return ApiResponse.success(adminService.updateMapEdge(id, request));
    }

    @DeleteMapping("/map/edges/{id}")
    public ApiResponse<Boolean> deleteMapEdge(@PathVariable Long id) {
        return ApiResponse.success(adminService.deleteMapEdge(id));
    }

    @GetMapping("/users")
    public ApiResponse<PageResultVO<AdminUserVO>> listUsers(
            @Valid @ModelAttribute AdminPageQuery query) {
        return ApiResponse.success(adminService.listUsers(query));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<AdminUserVO> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminStatusRequest request) {
        return ApiResponse.success(adminService.updateUserStatus(id, request));
    }

    @GetMapping("/diaries")
    public ApiResponse<PageResultVO<AdminDiaryVO>> listDiaries(
            @Valid @ModelAttribute AdminPageQuery query) {
        return ApiResponse.success(adminService.listDiaries(query));
    }

    @PutMapping("/diaries/{id}/status")
    public ApiResponse<AdminDiaryVO> updateDiaryStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminStatusRequest request) {
        return ApiResponse.success(adminService.updateDiaryStatus(id, request));
    }

    @PostMapping(value = "/import-batches/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportPreviewResult> previewImport(
            @RequestParam String targetTable,
            @RequestParam String sourceType,
            @RequestParam MultipartFile file) {
        ImportRequest request = importRequest(targetTable, sourceType, file);
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return ApiResponse.success(adminService.previewImport(request, reader));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.IMPORT_008);
        }
    }

    @PostMapping(value = "/import-batches", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportResult> runImport(
            @RequestParam String targetTable,
            @RequestParam String sourceType,
            @RequestParam MultipartFile file) {
        ImportRequest request = importRequest(targetTable, sourceType, file);
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return ApiResponse.success(adminService.runImport(request, reader));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.IMPORT_008);
        }
    }

    @GetMapping("/import-batches")
    public ApiResponse<PageResultVO<AdminImportBatchVO>> listImportBatches(
            @Valid @ModelAttribute AdminPageQuery query,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(adminService.listImportBatches(query, status));
    }

    @GetMapping("/import-batches/{batchId}/failures")
    public ApiResponse<PageResultVO<AdminImportFailureVO>> listImportFailures(
            @PathVariable Long batchId,
            @Valid @ModelAttribute AdminPageQuery query) {
        return ApiResponse.success(adminService.listImportFailures(batchId, query));
    }

    private ImportRequest importRequest(String targetTable, String sourceType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.IMPORT_001);
        }
        ImportRequest request = new ImportRequest();
        request.setTargetTable(targetTable);
        request.setSourceType(sourceType);
        request.setFileName(file.getOriginalFilename());
        request.setFileSize(file.getSize());
        return request;
    }
}
