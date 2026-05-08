package com.trip.service;

import com.trip.dto.request.AdminDestinationRequest;
import com.trip.dto.request.AdminFacilityRequest;
import com.trip.dto.request.AdminFoodRequest;
import com.trip.dto.request.AdminMapEdgeRequest;
import com.trip.dto.request.AdminMapNodeRequest;
import com.trip.dto.request.AdminPageQuery;
import com.trip.dto.request.AdminPlaceRequest;
import com.trip.dto.request.AdminStatusRequest;
import com.trip.dto.imports.ImportPreviewResult;
import com.trip.dto.imports.ImportRequest;
import com.trip.dto.imports.ImportResult;
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
import java.io.Reader;

/**
 * Admin 最小后台维护服务。
 */
public interface AdminService {

    PageResultVO<AdminDestinationVO> listDestinations(AdminPageQuery query);

    AdminDestinationVO createDestination(AdminDestinationRequest request);

    AdminDestinationVO updateDestination(Long id, AdminDestinationRequest request);

    Boolean deleteDestination(Long id);

    PageResultVO<AdminPlaceVO> listPlaces(AdminPageQuery query);

    AdminPlaceVO createPlace(AdminPlaceRequest request);

    AdminPlaceVO updatePlace(Long id, AdminPlaceRequest request);

    Boolean deletePlace(Long id);

    PageResultVO<AdminFacilityVO> listFacilities(AdminPageQuery query);

    AdminFacilityVO createFacility(AdminFacilityRequest request);

    AdminFacilityVO updateFacility(Long id, AdminFacilityRequest request);

    Boolean deleteFacility(Long id);

    PageResultVO<FoodVO> listFoods(AdminPageQuery query);

    FoodVO createFood(AdminFoodRequest request);

    FoodVO updateFood(Long id, AdminFoodRequest request);

    Boolean deleteFood(Long id);

    PageResultVO<AdminMapNodeVO> listMapNodes(AdminPageQuery query);

    AdminMapNodeVO createMapNode(AdminMapNodeRequest request);

    AdminMapNodeVO updateMapNode(Long id, AdminMapNodeRequest request);

    Boolean deleteMapNode(Long id);

    PageResultVO<AdminMapEdgeVO> listMapEdges(AdminPageQuery query);

    AdminMapEdgeVO createMapEdge(AdminMapEdgeRequest request);

    AdminMapEdgeVO updateMapEdge(Long id, AdminMapEdgeRequest request);

    Boolean deleteMapEdge(Long id);

    PageResultVO<AdminUserVO> listUsers(AdminPageQuery query);

    AdminUserVO updateUserStatus(Long id, AdminStatusRequest request);

    PageResultVO<AdminDiaryVO> listDiaries(AdminPageQuery query);

    AdminDiaryVO updateDiaryStatus(Long id, AdminStatusRequest request);

    ImportPreviewResult previewImport(ImportRequest request, Reader reader);

    ImportResult runImport(ImportRequest request, Reader reader);

    PageResultVO<AdminImportBatchVO> listImportBatches(AdminPageQuery query, String status);

    PageResultVO<AdminImportFailureVO> listImportFailures(Long batchId, AdminPageQuery query);
}
