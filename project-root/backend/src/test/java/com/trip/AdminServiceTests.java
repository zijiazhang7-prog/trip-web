package com.trip;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trip.common.ErrorCode;
import com.trip.dto.request.AdminDestinationRequest;
import com.trip.dto.request.AdminFacilityRequest;
import com.trip.dto.request.AdminFoodRequest;
import com.trip.dto.request.AdminMapEdgeRequest;
import com.trip.dto.request.AdminMapNodeRequest;
import com.trip.dto.request.AdminPageQuery;
import com.trip.dto.request.AdminPlaceRequest;
import com.trip.dto.request.AdminStatusRequest;
import com.trip.entity.Diary;
import com.trip.entity.Destination;
import com.trip.entity.Facility;
import com.trip.entity.Food;
import com.trip.entity.ImportBatch;
import com.trip.entity.ImportFailure;
import com.trip.entity.MapEdge;
import com.trip.entity.MapNode;
import com.trip.entity.Place;
import com.trip.entity.User;
import com.trip.exception.BusinessException;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.DiaryMapper;
import com.trip.mapper.FacilityMapper;
import com.trip.mapper.FoodMapper;
import com.trip.mapper.ImportBatchMapper;
import com.trip.mapper.ImportFailureMapper;
import com.trip.mapper.MapEdgeMapper;
import com.trip.mapper.MapNodeMapper;
import com.trip.mapper.PlaceMapper;
import com.trip.mapper.UserMapper;
import com.trip.service.ImportService;
import com.trip.service.impl.AdminServiceImpl;
import com.trip.vo.response.AdminDiaryVO;
import com.trip.vo.response.AdminDestinationVO;
import com.trip.vo.response.AdminFacilityVO;
import com.trip.vo.response.AdminImportFailureVO;
import com.trip.vo.response.AdminMapEdgeVO;
import com.trip.vo.response.AdminMapNodeVO;
import com.trip.vo.response.AdminPlaceVO;
import com.trip.vo.response.AdminUserVO;
import com.trip.vo.response.FoodVO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class AdminServiceTests {

    private final DestinationMapper destinationMapper = mock(DestinationMapper.class);
    private final PlaceMapper placeMapper = mock(PlaceMapper.class);
    private final FacilityMapper facilityMapper = mock(FacilityMapper.class);
    private final FoodMapper foodMapper = mock(FoodMapper.class);
    private final MapNodeMapper mapNodeMapper = mock(MapNodeMapper.class);
    private final MapEdgeMapper mapEdgeMapper = mock(MapEdgeMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final DiaryMapper diaryMapper = mock(DiaryMapper.class);
    private final ImportBatchMapper importBatchMapper = mock(ImportBatchMapper.class);
    private final ImportFailureMapper importFailureMapper = mock(ImportFailureMapper.class);
    private final ImportService importService = mock(ImportService.class);
    private final AdminServiceImpl adminService = new AdminServiceImpl(
            destinationMapper,
            placeMapper,
            facilityMapper,
            foodMapper,
            mapNodeMapper,
            mapEdgeMapper,
            userMapper,
            diaryMapper,
            importBatchMapper,
            importFailureMapper,
            importService);

    @Test
    void listDestinationsShouldReturnPagedResults() {
        Destination destination = destination(1L, "P1后台目的地");
        Page<Destination> page = new Page<>(1, 10);
        page.setRecords(List.of(destination));
        page.setTotal(1);
        when(destinationMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        AdminPageQuery query = new AdminPageQuery();
        query.setKeyword("后台");

        assertEquals(1, adminService.listDestinations(query).getTotal());
        verify(destinationMapper).selectPage(any(Page.class), any(Wrapper.class));
    }

    @Test
    void createDestinationShouldInsertEnabledRecord() {
        when(destinationMapper.insert(any(Destination.class))).thenAnswer(invocation -> {
            Destination destination = invocation.getArgument(0);
            destination.setId(1L);
            return 1;
        });
        when(destinationMapper.selectById(1L)).thenReturn(destination(1L, "P1后台目的地"));

        AdminDestinationVO result = adminService.createDestination(destinationRequest(" P1后台目的地 "));

        assertEquals(1L, result.getId());
        assertEquals("P1后台目的地", result.getName());
        verify(destinationMapper).insert(any(Destination.class));
    }

    @Test
    void deleteDestinationShouldDisableRecord() {
        when(destinationMapper.selectById(1L)).thenReturn(destination(1L, "待下架目的地"));

        assertEquals(true, adminService.deleteDestination(1L));

        verify(destinationMapper).updateById(any(Destination.class));
    }

    @Test
    void listPlacesShouldReturnPagedResults() {
        Page<Place> page = new Page<>(1, 10);
        page.setRecords(List.of(place(11L)));
        page.setTotal(1);
        when(placeMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        AdminPageQuery query = new AdminPageQuery();
        query.setDestinationId(1L);
        query.setType("building");
        query.setKeyword("图书馆");

        assertEquals(1, adminService.listPlaces(query).getTotal());
        verify(placeMapper).selectPage(any(Page.class), any(Wrapper.class));
    }

    @Test
    void createPlaceShouldRequireDestination() {
        when(destinationMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminService.createPlace(placeRequest()));

        assertEquals(ErrorCode.COMMON_003, exception.getErrorCode());
    }

    @Test
    void createPlaceShouldInsertRecord() {
        when(destinationMapper.selectById(1L)).thenReturn(destination(1L, "目的地"));
        when(placeMapper.insert(any(Place.class))).thenAnswer(invocation -> {
            Place place = invocation.getArgument(0);
            place.setId(11L);
            return 1;
        });
        when(placeMapper.selectById(11L)).thenReturn(place(11L));

        AdminPlaceVO result = adminService.createPlace(placeRequest());

        assertEquals(11L, result.getId());
        assertEquals("P1后台图书馆", result.getName());
        assertEquals("building", result.getPlaceType());
    }

    @Test
    void updatePlaceShouldUpdateRecord() {
        when(placeMapper.selectById(11L)).thenReturn(place(11L));
        when(destinationMapper.selectById(1L)).thenReturn(destination(1L, "目的地"));

        AdminPlaceVO result = adminService.updatePlace(11L, placeRequest());

        assertEquals(11L, result.getId());
        verify(placeMapper).updateById(any(Place.class));
    }

    @Test
    void deletePlaceShouldRejectReferencedFacility() {
        when(placeMapper.selectById(11L)).thenReturn(place(11L));
        when(facilityMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminService.deletePlace(11L));

        assertEquals(ErrorCode.COMMON_002, exception.getErrorCode());
    }

    @Test
    void deletePlaceShouldRejectReferencedMapNode() {
        when(placeMapper.selectById(11L)).thenReturn(place(11L));
        when(facilityMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapNodeMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminService.deletePlace(11L));

        assertEquals(ErrorCode.COMMON_002, exception.getErrorCode());
    }

    @Test
    void deletePlaceShouldDeleteUnreferencedRecord() {
        when(placeMapper.selectById(11L)).thenReturn(place(11L));
        when(facilityMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(mapNodeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(placeMapper.deleteById(11L)).thenReturn(1);

        assertEquals(true, adminService.deletePlace(11L));

        verify(placeMapper).deleteById(11L);
    }

    @Test
    void createFacilityShouldRequireDestination() {
        when(destinationMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminService.createFacility(facilityRequest()));

        assertEquals(ErrorCode.COMMON_003, exception.getErrorCode());
    }

    @Test
    void createFacilityShouldInsertEnabledRecord() {
        when(destinationMapper.selectById(1L)).thenReturn(destination(1L, "目的地"));
        when(facilityMapper.insert(any(Facility.class))).thenAnswer(invocation -> {
            Facility facility = invocation.getArgument(0);
            facility.setId(2L);
            return 1;
        });
        when(facilityMapper.selectById(2L)).thenReturn(facility(2L));

        AdminFacilityVO result = adminService.createFacility(facilityRequest());

        assertEquals(2L, result.getId());
        assertEquals("toilet", result.getFacilityType());
    }

    @Test
    void createFoodShouldInsertRecord() {
        when(destinationMapper.selectById(1L)).thenReturn(destination(1L, "目的地"));
        when(foodMapper.insert(any(Food.class))).thenAnswer(invocation -> {
            Food food = invocation.getArgument(0);
            food.setId(3L);
            return 1;
        });
        when(foodMapper.selectById(3L)).thenReturn(food(3L));

        FoodVO result = adminService.createFood(foodRequest());

        assertEquals(3L, result.getId());
        assertEquals("P1后台牛肉面", result.getName());
    }

    @Test
    void deleteFoodShouldDeleteById() {
        when(foodMapper.selectById(3L)).thenReturn(food(3L));
        when(foodMapper.deleteById(3L)).thenReturn(1);

        assertEquals(true, adminService.deleteFood(3L));

        verify(foodMapper).deleteById(3L);
    }

    @Test
    void createMapNodeShouldRequireDestinationAndInsertRecord() {
        when(destinationMapper.selectById(1L)).thenReturn(destination(1L, "目的地"));
        when(mapNodeMapper.insert(any(MapNode.class))).thenAnswer(invocation -> {
            MapNode node = invocation.getArgument(0);
            node.setId(4L);
            return 1;
        });
        when(mapNodeMapper.selectById(4L)).thenReturn(mapNode(4L, 1L));

        AdminMapNodeVO result = adminService.createMapNode(mapNodeRequest());

        assertEquals(4L, result.getId());
        assertEquals("P1后台节点", result.getNodeName());
    }

    @Test
    void deleteMapNodeShouldRejectReferencedNode() {
        when(mapNodeMapper.selectById(4L)).thenReturn(mapNode(4L, 1L));
        when(mapEdgeMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminService.deleteMapNode(4L));

        assertEquals(ErrorCode.COMMON_002, exception.getErrorCode());
    }

    @Test
    void createMapEdgeShouldRejectCrossDestinationNodes() {
        when(destinationMapper.selectById(1L)).thenReturn(destination(1L, "目的地"));
        when(mapNodeMapper.selectById(4L)).thenReturn(mapNode(4L, 1L));
        when(mapNodeMapper.selectById(5L)).thenReturn(mapNode(5L, 2L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminService.createMapEdge(mapEdgeRequest()));

        assertEquals(ErrorCode.ROUTE_009, exception.getErrorCode());
    }

    @Test
    void createMapEdgeShouldInsertValidEdge() {
        when(destinationMapper.selectById(1L)).thenReturn(destination(1L, "目的地"));
        when(mapNodeMapper.selectById(4L)).thenReturn(mapNode(4L, 1L));
        when(mapNodeMapper.selectById(5L)).thenReturn(mapNode(5L, 1L));
        when(mapEdgeMapper.insert(any(MapEdge.class))).thenAnswer(invocation -> {
            MapEdge edge = invocation.getArgument(0);
            edge.setId(6L);
            return 1;
        });
        when(mapEdgeMapper.selectById(6L)).thenReturn(mapEdge(6L));

        AdminMapEdgeVO result = adminService.createMapEdge(mapEdgeRequest());

        assertEquals(6L, result.getId());
        assertEquals(new BigDecimal("120.00"), result.getDistance());
    }

    @Test
    void updateUserStatusShouldOnlyChangeStatus() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));

        AdminStatusRequest request = new AdminStatusRequest();
        request.setStatus(0);
        AdminUserVO result = adminService.updateUserStatus(7L, request);

        assertEquals(0, result.getStatus());
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void updateDiaryStatusShouldOnlyChangeStatus() {
        when(diaryMapper.selectById(8L)).thenReturn(diary(8L));

        AdminStatusRequest request = new AdminStatusRequest();
        request.setStatus(0);
        AdminDiaryVO result = adminService.updateDiaryStatus(8L, request);

        assertEquals(0, result.getStatus());
        verify(diaryMapper).updateById(any(Diary.class));
    }

    @Test
    void listImportBatchesShouldReturnPagedResults() {
        Page<ImportBatch> page = new Page<>(1, 10);
        page.setRecords(List.of(importBatch(9L)));
        page.setTotal(1);
        when(importBatchMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        AdminPageQuery query = new AdminPageQuery();
        query.setKeyword("destinations");
        query.setType("destination");

        assertEquals(1, adminService.listImportBatches(query, "SUCCESS").getTotal());
        verify(importBatchMapper).selectPage(any(Page.class), any(Wrapper.class));
    }

    @Test
    void listImportFailuresShouldRequireExistingBatch() {
        when(importBatchMapper.selectById(404L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminService.listImportFailures(404L, new AdminPageQuery()));

        assertEquals(ErrorCode.COMMON_003, exception.getErrorCode());
    }

    @Test
    void listImportFailuresShouldReturnPagedResults() {
        when(importBatchMapper.selectById(9L)).thenReturn(importBatch(9L));
        Page<ImportFailure> page = new Page<>(1, 10);
        page.setRecords(List.of(importFailure(10L, 9L)));
        page.setTotal(1);
        when(importFailureMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        AdminImportFailureVO result = adminService.listImportFailures(9L, new AdminPageQuery())
                .getList()
                .get(0);

        assertEquals(10L, result.getId());
        assertEquals(2, result.getRowNo());
        assertEquals("导入字段缺失或字段名不匹配", result.getErrorMessage());
    }

    private AdminDestinationRequest destinationRequest(String name) {
        AdminDestinationRequest request = new AdminDestinationRequest();
        request.setName(name);
        request.setType("campus");
        request.setCategory("校园");
        request.setCity("北京");
        request.setDescription("后台维护测试");
        request.setHeatScore(new BigDecimal("80.00"));
        request.setRatingScore(new BigDecimal("4.50"));
        request.setTagJson("[\"校园\"]");
        return request;
    }

    private AdminFacilityRequest facilityRequest() {
        AdminFacilityRequest request = new AdminFacilityRequest();
        request.setDestinationId(1L);
        request.setName("P1后台厕所");
        request.setFacilityType("toilet");
        request.setDescription("后台设施维护测试");
        return request;
    }

    private AdminPlaceRequest placeRequest() {
        AdminPlaceRequest request = new AdminPlaceRequest();
        request.setDestinationId(1L);
        request.setName(" P1后台图书馆 ");
        request.setPlaceType("building");
        request.setDescription("后台场所维护测试");
        request.setLng(new BigDecimal("116.123456"));
        request.setLat(new BigDecimal("39.123456"));
        request.setFloorInfo("1F-5F");
        request.setHeatScore(new BigDecimal("86.00"));
        request.setRatingScore(new BigDecimal("4.70"));
        request.setOpenTimeRule("08:00-22:00");
        request.setSuggestedDurationMin(60);
        request.setCostLevel(1);
        return request;
    }

    private AdminFoodRequest foodRequest() {
        AdminFoodRequest request = new AdminFoodRequest();
        request.setDestinationId(1L);
        request.setName("P1后台牛肉面");
        request.setFoodType("面食");
        request.setShopName("后台窗口");
        request.setDescription("后台美食维护测试");
        request.setHeatScore(new BigDecimal("88.00"));
        request.setRatingScore(new BigDecimal("4.60"));
        request.setAvgPrice(new BigDecimal("18.00"));
        return request;
    }

    private AdminMapNodeRequest mapNodeRequest() {
        AdminMapNodeRequest request = new AdminMapNodeRequest();
        request.setDestinationId(1L);
        request.setNodeName(" P1后台节点 ");
        request.setNodeType("intersection");
        request.setLng(new BigDecimal("116.123456"));
        request.setLat(new BigDecimal("39.123456"));
        return request;
    }

    private AdminMapEdgeRequest mapEdgeRequest() {
        AdminMapEdgeRequest request = new AdminMapEdgeRequest();
        request.setDestinationId(1L);
        request.setFromNodeId(4L);
        request.setToNodeId(5L);
        request.setDistance(new BigDecimal("120.00"));
        request.setIdealSpeed(new BigDecimal("1.20"));
        request.setCrowdFactor(new BigDecimal("1.00"));
        request.setTransportType("walk");
        request.setEdgeType("road");
        request.setBidirectionalFlag(0);
        return request;
    }

    private Destination destination(Long id, String name) {
        Destination destination = new Destination();
        destination.setId(id);
        destination.setName(name);
        destination.setType("campus");
        destination.setCategory("校园");
        destination.setCity("北京");
        destination.setDescription("测试目的地");
        destination.setHeatScore(new BigDecimal("80.00"));
        destination.setRatingScore(new BigDecimal("4.50"));
        destination.setStatus(1);
        return destination;
    }

    private Facility facility(Long id) {
        Facility facility = new Facility();
        facility.setId(id);
        facility.setDestinationId(1L);
        facility.setName("P1后台厕所");
        facility.setFacilityType("toilet");
        facility.setDescription("测试设施");
        facility.setStatus(1);
        return facility;
    }

    private Place place(Long id) {
        Place place = new Place();
        place.setId(id);
        place.setDestinationId(1L);
        place.setName("P1后台图书馆");
        place.setPlaceType("building");
        place.setDescription("测试场所");
        place.setLng(new BigDecimal("116.123456"));
        place.setLat(new BigDecimal("39.123456"));
        place.setFloorInfo("1F-5F");
        place.setHeatScore(new BigDecimal("86.00"));
        place.setRatingScore(new BigDecimal("4.70"));
        place.setOpenTimeRule("08:00-22:00");
        place.setSuggestedDurationMin(60);
        place.setCostLevel(1);
        return place;
    }

    private Food food(Long id) {
        Food food = new Food();
        food.setId(id);
        food.setDestinationId(1L);
        food.setName("P1后台牛肉面");
        food.setFoodType("面食");
        food.setShopName("后台窗口");
        food.setDescription("测试美食");
        food.setHeatScore(new BigDecimal("88.00"));
        food.setRatingScore(new BigDecimal("4.60"));
        food.setAvgPrice(new BigDecimal("18.00"));
        return food;
    }

    private MapNode mapNode(Long id, Long destinationId) {
        MapNode node = new MapNode();
        node.setId(id);
        node.setDestinationId(destinationId);
        node.setNodeName("P1后台节点");
        node.setNodeType("intersection");
        return node;
    }

    private MapEdge mapEdge(Long id) {
        MapEdge edge = new MapEdge();
        edge.setId(id);
        edge.setDestinationId(1L);
        edge.setFromNodeId(4L);
        edge.setToNodeId(5L);
        edge.setDistance(new BigDecimal("120.00"));
        edge.setTransportType("walk");
        edge.setEdgeType("road");
        edge.setBidirectionalFlag(0);
        return edge;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("admin-user");
        user.setNickname("管理员");
        user.setRole("admin");
        user.setStatus(1);
        return user;
    }

    private Diary diary(Long id) {
        Diary diary = new Diary();
        diary.setId(id);
        diary.setUserId(7L);
        diary.setDestinationId(1L);
        diary.setTitle("后台日记");
        diary.setVisibility("public");
        diary.setStatus(1);
        return diary;
    }

    private ImportBatch importBatch(Long id) {
        ImportBatch batch = new ImportBatch();
        batch.setId(id);
        batch.setBatchName("destination_20260506210000");
        batch.setTargetTable("destination");
        batch.setSourceType("csv");
        batch.setFileName("destinations.csv");
        batch.setFileSize(1024L);
        batch.setStatus("SUCCESS");
        batch.setTotalRows(1L);
        batch.setSuccessRows(1L);
        batch.setFailedRows(0L);
        return batch;
    }

    private ImportFailure importFailure(Long id, Long batchId) {
        ImportFailure failure = new ImportFailure();
        failure.setId(id);
        failure.setBatchId(batchId);
        failure.setRowNo(2);
        failure.setFieldName("name");
        failure.setErrorMessage("导入字段缺失或字段名不匹配");
        failure.setRawDataJson("{\"type\":\"campus\"}");
        return failure;
    }
}
