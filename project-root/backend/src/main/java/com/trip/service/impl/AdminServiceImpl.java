package com.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trip.common.ErrorCode;
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
import com.trip.engine.index.IndexDocument;
import com.trip.engine.index.IndexNamespace;
import com.trip.entity.Destination;
import com.trip.entity.Diary;
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
import com.trip.service.AdminService;
import com.trip.service.ImportService;
import com.trip.service.IndexMaintenanceService;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Admin 最小后台实现：先维护目的地、设施和美食三类基础数据。
 */
@Service
public class AdminServiceImpl implements AdminService {

    private static final int ENABLED_STATUS = 1;
    private static final int DISABLED_STATUS = 0;
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String VISIBILITY_PUBLIC = "public";

    private final DestinationMapper destinationMapper;
    private final PlaceMapper placeMapper;
    private final FacilityMapper facilityMapper;
    private final FoodMapper foodMapper;
    private final MapNodeMapper mapNodeMapper;
    private final MapEdgeMapper mapEdgeMapper;
    private final UserMapper userMapper;
    private final DiaryMapper diaryMapper;
    private final ImportBatchMapper importBatchMapper;
    private final ImportFailureMapper importFailureMapper;
    private final ImportService importService;
    private final IndexMaintenanceService indexMaintenanceService;

    public AdminServiceImpl(
            DestinationMapper destinationMapper,
            PlaceMapper placeMapper,
            FacilityMapper facilityMapper,
            FoodMapper foodMapper,
            MapNodeMapper mapNodeMapper,
            MapEdgeMapper mapEdgeMapper,
            UserMapper userMapper,
            DiaryMapper diaryMapper,
            ImportBatchMapper importBatchMapper,
            ImportFailureMapper importFailureMapper,
            ImportService importService,
            IndexMaintenanceService indexMaintenanceService) {
        this.destinationMapper = destinationMapper;
        this.placeMapper = placeMapper;
        this.facilityMapper = facilityMapper;
        this.foodMapper = foodMapper;
        this.mapNodeMapper = mapNodeMapper;
        this.mapEdgeMapper = mapEdgeMapper;
        this.userMapper = userMapper;
        this.diaryMapper = diaryMapper;
        this.importBatchMapper = importBatchMapper;
        this.importFailureMapper = importFailureMapper;
        this.importService = importService;
        this.indexMaintenanceService = indexMaintenanceService;
    }

    @Override
    public PageResultVO<AdminDestinationVO> listDestinations(AdminPageQuery query) {
        AdminPageQuery safeQuery = safeQuery(query);
        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<>();
        String keyword = normalize(safeQuery.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(Destination::getName, keyword)
                    .or()
                    .like(Destination::getCity, keyword)
                    .or()
                    .like(Destination::getCategory, keyword));
        }
        String type = normalize(safeQuery.getType());
        if (StringUtils.hasText(type)) {
            wrapper.eq(Destination::getType, type);
        }
        wrapper.orderByDesc(Destination::getCreatedAt).orderByAsc(Destination::getId);
        IPage<Destination> page = destinationMapper.selectPage(page(safeQuery), wrapper);
        return PageResultVO.of(
                page.getRecords().stream().map(AdminDestinationVO::from).toList(),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages());
    }

    @Override
    @Transactional
    public AdminDestinationVO createDestination(AdminDestinationRequest request) {
        Destination destination = new Destination();
        fillDestination(destination, request);
        destination.setStatus(ENABLED_STATUS);
        destination.setCreatedAt(LocalDateTime.now());
        destinationMapper.insert(destination);
        indexMaintenanceService.invalidate(IndexNamespace.DESTINATION_NAME);
        return AdminDestinationVO.from(destinationMapper.selectById(destination.getId()));
    }

    @Override
    @Transactional
    public AdminDestinationVO updateDestination(Long id, AdminDestinationRequest request) {
        Destination destination = requireDestination(id);
        fillDestination(destination, request);
        destinationMapper.updateById(destination);
        indexMaintenanceService.invalidate(IndexNamespace.DESTINATION_NAME);
        return AdminDestinationVO.from(destinationMapper.selectById(id));
    }

    @Override
    @Transactional
    public Boolean deleteDestination(Long id) {
        Destination destination = requireDestination(id);
        destination.setStatus(DISABLED_STATUS);
        destinationMapper.updateById(destination);
        indexMaintenanceService.invalidate(IndexNamespace.DESTINATION_NAME);
        return true;
    }

    @Override
    public PageResultVO<AdminPlaceVO> listPlaces(AdminPageQuery query) {
        AdminPageQuery safeQuery = safeQuery(query);
        LambdaQueryWrapper<Place> wrapper = new LambdaQueryWrapper<>();
        if (safeQuery.getDestinationId() != null) {
            wrapper.eq(Place::getDestinationId, safeQuery.getDestinationId());
        }
        String type = normalize(safeQuery.getType());
        if (StringUtils.hasText(type)) {
            wrapper.eq(Place::getPlaceType, type);
        }
        String keyword = normalize(safeQuery.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(Place::getName, keyword)
                    .or()
                    .like(Place::getDescription, keyword));
        }
        wrapper.orderByAsc(Place::getId);
        IPage<Place> page = placeMapper.selectPage(page(safeQuery), wrapper);
        return PageResultVO.of(
                page.getRecords().stream().map(AdminPlaceVO::from).toList(),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages());
    }

    @Override
    @Transactional
    public AdminPlaceVO createPlace(AdminPlaceRequest request) {
        requireDestination(request.getDestinationId());
        Place place = new Place();
        fillPlace(place, request);
        placeMapper.insert(place);
        return AdminPlaceVO.from(placeMapper.selectById(place.getId()));
    }

    @Override
    @Transactional
    public AdminPlaceVO updatePlace(Long id, AdminPlaceRequest request) {
        Place place = requirePlace(id);
        requireDestination(request.getDestinationId());
        fillPlace(place, request);
        placeMapper.updateById(place);
        return AdminPlaceVO.from(placeMapper.selectById(id));
    }

    @Override
    @Transactional
    public Boolean deletePlace(Long id) {
        requirePlace(id);
        Long facilityCount = facilityMapper.selectCount(new LambdaQueryWrapper<Facility>()
                .eq(Facility::getPlaceId, id));
        if (facilityCount != null && facilityCount > 0) {
            throw new BusinessException(ErrorCode.COMMON_002);
        }
        Long mapNodeCount = mapNodeMapper.selectCount(new LambdaQueryWrapper<MapNode>()
                .eq(MapNode::getNodeType, "place")
                .eq(MapNode::getRefId, id));
        if (mapNodeCount != null && mapNodeCount > 0) {
            throw new BusinessException(ErrorCode.COMMON_002);
        }
        return placeMapper.deleteById(id) > 0;
    }

    @Override
    public PageResultVO<AdminFacilityVO> listFacilities(AdminPageQuery query) {
        AdminPageQuery safeQuery = safeQuery(query);
        LambdaQueryWrapper<Facility> wrapper = new LambdaQueryWrapper<>();
        if (safeQuery.getDestinationId() != null) {
            wrapper.eq(Facility::getDestinationId, safeQuery.getDestinationId());
        }
        String type = normalize(safeQuery.getType());
        if (StringUtils.hasText(type)) {
            wrapper.eq(Facility::getFacilityType, type);
        }
        String keyword = normalize(safeQuery.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(Facility::getName, keyword)
                    .or()
                    .like(Facility::getDescription, keyword));
        }
        wrapper.orderByAsc(Facility::getId);
        IPage<Facility> page = facilityMapper.selectPage(page(safeQuery), wrapper);
        return PageResultVO.of(
                page.getRecords().stream().map(AdminFacilityVO::from).toList(),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages());
    }

    @Override
    @Transactional
    public AdminFacilityVO createFacility(AdminFacilityRequest request) {
        requireDestination(request.getDestinationId());
        Facility facility = new Facility();
        fillFacility(facility, request);
        facility.setStatus(ENABLED_STATUS);
        facilityMapper.insert(facility);
        return AdminFacilityVO.from(facilityMapper.selectById(facility.getId()));
    }

    @Override
    @Transactional
    public AdminFacilityVO updateFacility(Long id, AdminFacilityRequest request) {
        Facility facility = requireFacility(id);
        requireDestination(request.getDestinationId());
        fillFacility(facility, request);
        facilityMapper.updateById(facility);
        return AdminFacilityVO.from(facilityMapper.selectById(id));
    }

    @Override
    @Transactional
    public Boolean deleteFacility(Long id) {
        Facility facility = requireFacility(id);
        facility.setStatus(DISABLED_STATUS);
        facilityMapper.updateById(facility);
        return true;
    }

    @Override
    public PageResultVO<FoodVO> listFoods(AdminPageQuery query) {
        AdminPageQuery safeQuery = safeQuery(query);
        LambdaQueryWrapper<Food> wrapper = new LambdaQueryWrapper<>();
        if (safeQuery.getDestinationId() != null) {
            wrapper.eq(Food::getDestinationId, safeQuery.getDestinationId());
        }
        String type = normalize(safeQuery.getType());
        if (StringUtils.hasText(type)) {
            wrapper.eq(Food::getFoodType, type);
        }
        String keyword = normalize(safeQuery.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(Food::getName, keyword)
                    .or()
                    .like(Food::getShopName, keyword)
                    .or()
                    .like(Food::getDescription, keyword));
        }
        wrapper.orderByAsc(Food::getId);
        IPage<Food> page = foodMapper.selectPage(page(safeQuery), wrapper);
        return PageResultVO.of(
                page.getRecords().stream().map(FoodVO::from).toList(),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages());
    }

    @Override
    @Transactional
    public FoodVO createFood(AdminFoodRequest request) {
        requireDestination(request.getDestinationId());
        Food food = new Food();
        fillFood(food, request);
        foodMapper.insert(food);
        invalidateFoodIndexes();
        return FoodVO.from(foodMapper.selectById(food.getId()));
    }

    @Override
    @Transactional
    public FoodVO updateFood(Long id, AdminFoodRequest request) {
        Food food = requireFood(id);
        requireDestination(request.getDestinationId());
        fillFood(food, request);
        foodMapper.updateById(food);
        invalidateFoodIndexes();
        return FoodVO.from(foodMapper.selectById(id));
    }

    @Override
    @Transactional
    public Boolean deleteFood(Long id) {
        requireFood(id);
        boolean deleted = foodMapper.deleteById(id) > 0;
        if (deleted) {
            invalidateFoodIndexes();
        }
        return deleted;
    }

    @Override
    public PageResultVO<AdminMapNodeVO> listMapNodes(AdminPageQuery query) {
        AdminPageQuery safeQuery = safeQuery(query);
        LambdaQueryWrapper<MapNode> wrapper = new LambdaQueryWrapper<>();
        if (safeQuery.getDestinationId() != null) {
            wrapper.eq(MapNode::getDestinationId, safeQuery.getDestinationId());
        }
        String type = normalize(safeQuery.getType());
        if (StringUtils.hasText(type)) {
            wrapper.eq(MapNode::getNodeType, type);
        }
        String keyword = normalize(safeQuery.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.like(MapNode::getNodeName, keyword);
        }
        wrapper.orderByAsc(MapNode::getId);
        IPage<MapNode> page = mapNodeMapper.selectPage(page(safeQuery), wrapper);
        return PageResultVO.of(
                page.getRecords().stream().map(AdminMapNodeVO::from).toList(),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages());
    }

    @Override
    @Transactional
    public AdminMapNodeVO createMapNode(AdminMapNodeRequest request) {
        requireDestination(request.getDestinationId());
        MapNode node = new MapNode();
        fillMapNode(node, request);
        mapNodeMapper.insert(node);
        return AdminMapNodeVO.from(mapNodeMapper.selectById(node.getId()));
    }

    @Override
    @Transactional
    public AdminMapNodeVO updateMapNode(Long id, AdminMapNodeRequest request) {
        MapNode node = requireMapNode(id);
        requireDestination(request.getDestinationId());
        fillMapNode(node, request);
        mapNodeMapper.updateById(node);
        return AdminMapNodeVO.from(mapNodeMapper.selectById(id));
    }

    @Override
    @Transactional
    public Boolean deleteMapNode(Long id) {
        requireMapNode(id);
        Long edgeCount = mapEdgeMapper.selectCount(new LambdaQueryWrapper<MapEdge>()
                .eq(MapEdge::getFromNodeId, id)
                .or()
                .eq(MapEdge::getToNodeId, id));
        if (edgeCount != null && edgeCount > 0) {
            throw new BusinessException(ErrorCode.COMMON_002);
        }
        return mapNodeMapper.deleteById(id) > 0;
    }

    @Override
    public PageResultVO<AdminMapEdgeVO> listMapEdges(AdminPageQuery query) {
        AdminPageQuery safeQuery = safeQuery(query);
        LambdaQueryWrapper<MapEdge> wrapper = new LambdaQueryWrapper<>();
        if (safeQuery.getDestinationId() != null) {
            wrapper.eq(MapEdge::getDestinationId, safeQuery.getDestinationId());
        }
        String type = normalize(safeQuery.getType());
        if (StringUtils.hasText(type)) {
            wrapper.eq(MapEdge::getTransportType, type);
        }
        wrapper.orderByAsc(MapEdge::getId);
        IPage<MapEdge> page = mapEdgeMapper.selectPage(page(safeQuery), wrapper);
        return PageResultVO.of(
                page.getRecords().stream().map(AdminMapEdgeVO::from).toList(),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages());
    }

    @Override
    @Transactional
    public AdminMapEdgeVO createMapEdge(AdminMapEdgeRequest request) {
        validateMapEdgeRequest(request);
        MapEdge edge = new MapEdge();
        fillMapEdge(edge, request);
        mapEdgeMapper.insert(edge);
        return AdminMapEdgeVO.from(mapEdgeMapper.selectById(edge.getId()));
    }

    @Override
    @Transactional
    public AdminMapEdgeVO updateMapEdge(Long id, AdminMapEdgeRequest request) {
        MapEdge edge = requireMapEdge(id);
        validateMapEdgeRequest(request);
        fillMapEdge(edge, request);
        mapEdgeMapper.updateById(edge);
        return AdminMapEdgeVO.from(mapEdgeMapper.selectById(id));
    }

    @Override
    @Transactional
    public Boolean deleteMapEdge(Long id) {
        requireMapEdge(id);
        return mapEdgeMapper.deleteById(id) > 0;
    }

    @Override
    public PageResultVO<AdminUserVO> listUsers(AdminPageQuery query) {
        AdminPageQuery safeQuery = safeQuery(query);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        String type = normalize(safeQuery.getType());
        if (StringUtils.hasText(type)) {
            wrapper.eq(User::getRole, type);
        }
        String keyword = normalize(safeQuery.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt).orderByAsc(User::getId);
        IPage<User> page = userMapper.selectPage(page(safeQuery), wrapper);
        return PageResultVO.of(
                page.getRecords().stream().map(AdminUserVO::from).toList(),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages());
    }

    @Override
    @Transactional
    public AdminUserVO updateUserStatus(Long id, AdminStatusRequest request) {
        User user = requireUser(id);
        user.setStatus(status(request));
        userMapper.updateById(user);
        return AdminUserVO.from(userMapper.selectById(id));
    }

    @Override
    public PageResultVO<AdminDiaryVO> listDiaries(AdminPageQuery query) {
        AdminPageQuery safeQuery = safeQuery(query);
        LambdaQueryWrapper<Diary> wrapper = new LambdaQueryWrapper<>();
        if (safeQuery.getDestinationId() != null) {
            wrapper.eq(Diary::getDestinationId, safeQuery.getDestinationId());
        }
        String keyword = normalize(safeQuery.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Diary::getTitle, keyword);
        }
        wrapper.orderByDesc(Diary::getCreatedAt).orderByAsc(Diary::getId);
        IPage<Diary> page = diaryMapper.selectPage(page(safeQuery), wrapper);
        return PageResultVO.of(
                page.getRecords().stream().map(AdminDiaryVO::from).toList(),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages());
    }

    @Override
    @Transactional
    public AdminDiaryVO updateDiaryStatus(Long id, AdminStatusRequest request) {
        Diary diary = requireDiary(id);
        diary.setStatus(status(request));
        diaryMapper.updateById(diary);
        indexMaintenanceService.invalidateAfterCommit(IndexNamespace.DIARY_TITLE);
        if (diary.getStatus() == ENABLED_STATUS
                && VISIBILITY_PUBLIC.equals(diary.getVisibility())) {
            indexMaintenanceService.upsertAfterCommit(
                    IndexNamespace.DIARY_CONTENT,
                    new IndexDocument(diary.getId(), diary.getContentText()));
        } else {
            indexMaintenanceService.removeAfterCommit(IndexNamespace.DIARY_CONTENT, diary.getId());
        }
        return AdminDiaryVO.from(diaryMapper.selectById(id));
    }

    @Override
    public ImportPreviewResult previewImport(ImportRequest request, Reader reader) {
        return importService.previewImport(request, reader);
    }

    @Override
    public ImportResult runImport(ImportRequest request, Reader reader) {
        return importService.runImport(request, reader);
    }

    @Override
    public PageResultVO<AdminImportBatchVO> listImportBatches(AdminPageQuery query, String status) {
        AdminPageQuery safeQuery = safeQuery(query);
        LambdaQueryWrapper<ImportBatch> wrapper = new LambdaQueryWrapper<>();
        String keyword = normalize(safeQuery.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(ImportBatch::getBatchName, keyword)
                    .or()
                    .like(ImportBatch::getFileName, keyword));
        }
        String targetTable = normalize(safeQuery.getType());
        if (StringUtils.hasText(targetTable)) {
            wrapper.eq(ImportBatch::getTargetTable, targetTable);
        }
        String safeStatus = normalize(status);
        if (StringUtils.hasText(safeStatus)) {
            wrapper.eq(ImportBatch::getStatus, safeStatus);
        }
        wrapper.orderByDesc(ImportBatch::getCreatedAt).orderByDesc(ImportBatch::getId);
        IPage<ImportBatch> page = importBatchMapper.selectPage(page(safeQuery), wrapper);
        return PageResultVO.of(
                page.getRecords().stream().map(AdminImportBatchVO::from).toList(),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages());
    }

    @Override
    public PageResultVO<AdminImportFailureVO> listImportFailures(Long batchId, AdminPageQuery query) {
        requireImportBatch(batchId);
        AdminPageQuery safeQuery = safeQuery(query);
        LambdaQueryWrapper<ImportFailure> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImportFailure::getBatchId, batchId)
                .orderByAsc(ImportFailure::getRowNo)
                .orderByAsc(ImportFailure::getId);
        IPage<ImportFailure> page = importFailureMapper.selectPage(page(safeQuery), wrapper);
        return PageResultVO.of(
                page.getRecords().stream().map(AdminImportFailureVO::from).toList(),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages());
    }

    private void fillDestination(Destination destination, AdminDestinationRequest request) {
        destination.setName(normalizeRequired(request.getName()));
        destination.setType(normalizeRequired(request.getType()));
        destination.setCategory(normalize(request.getCategory()));
        destination.setCity(normalize(request.getCity()));
        destination.setDescription(normalize(request.getDescription()));
        destination.setHeatScore(defaultScore(request.getHeatScore()));
        destination.setRatingScore(defaultScore(request.getRatingScore()));
        destination.setTagJson(normalize(request.getTagJson()));
        destination.setCoverUrl(normalize(request.getCoverUrl()));
    }

    private void fillFacility(Facility facility, AdminFacilityRequest request) {
        facility.setDestinationId(request.getDestinationId());
        facility.setPlaceId(request.getPlaceId());
        facility.setName(normalizeRequired(request.getName()));
        facility.setFacilityType(normalizeRequired(request.getFacilityType()));
        facility.setDescription(normalize(request.getDescription()));
        facility.setAddress(normalize(request.getAddress()));
        facility.setTel(normalize(request.getTel()));
        facility.setCoverUrl(normalize(request.getCoverUrl()));
        facility.setLng(request.getLng());
        facility.setLat(request.getLat());
    }

    private void fillPlace(Place place, AdminPlaceRequest request) {
        place.setDestinationId(request.getDestinationId());
        place.setName(normalizeRequired(request.getName()));
        place.setPlaceType(normalizeRequired(request.getPlaceType()));
        place.setDescription(normalize(request.getDescription()));
        place.setLng(request.getLng());
        place.setLat(request.getLat());
        place.setFloorInfo(normalize(request.getFloorInfo()));
        place.setHeatScore(defaultScore(request.getHeatScore()));
        place.setRatingScore(defaultScore(request.getRatingScore()));
        place.setOpenTimeRule(normalize(request.getOpenTimeRule()));
        place.setSuggestedDurationMin(request.getSuggestedDurationMin());
        place.setCostLevel(request.getCostLevel());
    }

    private void fillFood(Food food, AdminFoodRequest request) {
        food.setDestinationId(request.getDestinationId());
        food.setFacilityId(request.getFacilityId());
        food.setName(normalizeRequired(request.getName()));
        food.setFoodType(normalize(request.getFoodType()));
        food.setShopName(normalize(request.getShopName()));
        food.setDescription(normalize(request.getDescription()));
        food.setHeatScore(defaultScore(request.getHeatScore()));
        food.setRatingScore(defaultScore(request.getRatingScore()));
        food.setAvgPrice(request.getAvgPrice());
        food.setCoverUrl(normalize(request.getCoverUrl()));
        food.setLng(request.getLng());
        food.setLat(request.getLat());
    }

    private void fillMapNode(MapNode node, AdminMapNodeRequest request) {
        node.setDestinationId(request.getDestinationId());
        node.setNodeName(normalizeRequired(request.getNodeName()));
        node.setNodeType(normalizeRequired(request.getNodeType()));
        node.setRefId(request.getRefId());
        node.setLng(request.getLng());
        node.setLat(request.getLat());
        node.setFloorNo(request.getFloorNo());
    }

    private void fillMapEdge(MapEdge edge, AdminMapEdgeRequest request) {
        edge.setDestinationId(request.getDestinationId());
        edge.setFromNodeId(request.getFromNodeId());
        edge.setToNodeId(request.getToNodeId());
        edge.setDistance(request.getDistance());
        edge.setIdealSpeed(request.getIdealSpeed());
        edge.setCrowdFactor(request.getCrowdFactor());
        edge.setTransportType(normalize(request.getTransportType()));
        edge.setEdgeType(normalize(request.getEdgeType()));
        edge.setBidirectionalFlag(request.getBidirectionalFlag() == null ? 0 : request.getBidirectionalFlag());
    }

    private void validateMapEdgeRequest(AdminMapEdgeRequest request) {
        requireDestination(request.getDestinationId());
        MapNode fromNode = requireMapNode(request.getFromNodeId());
        MapNode toNode = requireMapNode(request.getToNodeId());
        if (!request.getDestinationId().equals(fromNode.getDestinationId())
                || !request.getDestinationId().equals(toNode.getDestinationId())) {
            throw new BusinessException(ErrorCode.ROUTE_009);
        }
    }

    private Destination requireDestination(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        Destination destination = destinationMapper.selectById(id);
        if (destination == null) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
        return destination;
    }

    private Facility requireFacility(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        Facility facility = facilityMapper.selectById(id);
        if (facility == null) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
        return facility;
    }

    private Place requirePlace(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        Place place = placeMapper.selectById(id);
        if (place == null) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
        return place;
    }

    private Food requireFood(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        Food food = foodMapper.selectById(id);
        if (food == null) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
        return food;
    }

    private MapNode requireMapNode(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        MapNode node = mapNodeMapper.selectById(id);
        if (node == null) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
        return node;
    }

    private MapEdge requireMapEdge(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        MapEdge edge = mapEdgeMapper.selectById(id);
        if (edge == null) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
        return edge;
    }

    private User requireUser(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
        return user;
    }

    private Diary requireDiary(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        Diary diary = diaryMapper.selectById(id);
        if (diary == null) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
        return diary;
    }

    private ImportBatch requireImportBatch(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        ImportBatch batch = importBatchMapper.selectById(id);
        if (batch == null) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
        return batch;
    }

    private int status(AdminStatusRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        int status = request.getStatus();
        if (status != ENABLED_STATUS && status != DISABLED_STATUS) {
            throw new BusinessException(ErrorCode.COMMON_002);
        }
        return status;
    }

    private <T> Page<T> page(AdminPageQuery query) {
        return new Page<>(pageNum(query.getPageNum()), pageSize(query.getPageSize()));
    }

    private AdminPageQuery safeQuery(AdminPageQuery query) {
        return query == null ? new AdminPageQuery() : query;
    }

    private int pageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private int pageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private BigDecimal defaultScore(BigDecimal score) {
        return score == null ? BigDecimal.ZERO : score;
    }

    private String normalizeRequired(String value) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private void invalidateFoodIndexes() {
        indexMaintenanceService.invalidate(IndexNamespace.FOOD_NAME);
        indexMaintenanceService.invalidate(IndexNamespace.FOOD_SHOP_NAME);
    }
}
