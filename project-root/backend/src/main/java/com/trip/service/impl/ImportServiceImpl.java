package com.trip.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.common.ErrorCode;
import com.trip.dto.imports.ImportPreviewResult;
import com.trip.dto.imports.ImportRequest;
import com.trip.dto.imports.ImportResult;
import com.trip.engine.index.IndexNamespace;
import com.trip.entity.Destination;
import com.trip.entity.Facility;
import com.trip.entity.Food;
import com.trip.entity.ImportBatch;
import com.trip.entity.ImportFailure;
import com.trip.entity.MapEdge;
import com.trip.entity.MapNode;
import com.trip.entity.Place;
import com.trip.exception.BusinessException;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.FacilityMapper;
import com.trip.mapper.FoodMapper;
import com.trip.mapper.ImportBatchMapper;
import com.trip.mapper.ImportFailureMapper;
import com.trip.mapper.MapEdgeMapper;
import com.trip.mapper.MapNodeMapper;
import com.trip.mapper.PlaceMapper;
import com.trip.service.ImportService;
import com.trip.service.IndexMaintenanceService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ImportServiceImpl implements ImportService {

    private static final long MAX_IMPORT_FILE_SIZE = 20L * 1024 * 1024;
    private static final int MAX_IMPORT_ROWS = 1000;
    private static final int ENABLED_STATUS = 1;
    private static final String STATUS_PREVIEW_ONLY = "PREVIEW_ONLY";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final DateTimeFormatter BATCH_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> SUPPORTED_TARGET_TABLES = Set.of(
            "destination", "place", "facility", "food", "map_node", "map_edge");
    private static final Map<String, String> SOURCE_TYPE_EXTENSIONS = Map.of(
            "csv", ".csv",
            "json", ".json");

    private final ObjectMapper objectMapper;
    private final DestinationMapper destinationMapper;
    private final PlaceMapper placeMapper;
    private final FacilityMapper facilityMapper;
    private final FoodMapper foodMapper;
    private final MapNodeMapper mapNodeMapper;
    private final MapEdgeMapper mapEdgeMapper;
    private final ImportBatchMapper importBatchMapper;
    private final ImportFailureMapper importFailureMapper;
    private final IndexMaintenanceService indexMaintenanceService;

    public ImportServiceImpl(
            ObjectMapper objectMapper,
            DestinationMapper destinationMapper,
            PlaceMapper placeMapper,
            FacilityMapper facilityMapper,
            FoodMapper foodMapper,
            MapNodeMapper mapNodeMapper,
            MapEdgeMapper mapEdgeMapper,
            ImportBatchMapper importBatchMapper,
            ImportFailureMapper importFailureMapper,
            IndexMaintenanceService indexMaintenanceService) {
        this.objectMapper = objectMapper;
        this.destinationMapper = destinationMapper;
        this.placeMapper = placeMapper;
        this.facilityMapper = facilityMapper;
        this.foodMapper = foodMapper;
        this.mapNodeMapper = mapNodeMapper;
        this.mapEdgeMapper = mapEdgeMapper;
        this.importBatchMapper = importBatchMapper;
        this.importFailureMapper = importFailureMapper;
        this.indexMaintenanceService = indexMaintenanceService;
    }

    /**
     * 校验导入文件基础元信息。
     *
     * <p>核心导入流程只依赖 Reader，不绑定 Spring MultipartFile，便于后续复用命令行、脚本或对象存储来源。</p>
     */
    @Override
    public void validateImportFile(ImportRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }

        String targetTable = normalizeLower(request.getTargetTable());
        if (!StringUtils.hasText(targetTable) || !SUPPORTED_TARGET_TABLES.contains(targetTable)) {
            throw new BusinessException(ErrorCode.IMPORT_003);
        }

        String sourceType = normalizeLower(request.getSourceType());
        String expectedExtension = SOURCE_TYPE_EXTENSIONS.get(sourceType);
        if (!StringUtils.hasText(sourceType) || expectedExtension == null) {
            throw new BusinessException(ErrorCode.IMPORT_002);
        }

        String fileName = normalize(request.getFileName());
        if (!StringUtils.hasText(fileName)) {
            throw new BusinessException(ErrorCode.IMPORT_001);
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(expectedExtension)) {
            throw new BusinessException(ErrorCode.IMPORT_002);
        }

        Long fileSize = request.getFileSize();
        if (fileSize == null || fileSize <= 0 || fileSize > MAX_IMPORT_FILE_SIZE) {
            throw new BusinessException(ErrorCode.IMPORT_004);
        }
    }

    @Override
    public ImportPreviewResult previewImport(ImportRequest request, Reader reader) {
        validateImportFile(request);
        List<ImportRow> rows = parseRows(request, reader);

        ImportPreviewResult result = new ImportPreviewResult();
        result.setBatchName(batchName(request));
        result.setTargetTable(normalizeLower(request.getTargetTable()));
        result.setSourceType(normalizeLower(request.getSourceType()));
        result.setFileName(normalize(request.getFileName()));
        result.setStatus(STATUS_PREVIEW_ONLY);
        result.setTotalRows(rows.size());
        List<String> errors = validateRows(normalizeLower(request.getTargetTable()), rows);
        if (!errors.isEmpty()) {
            result.getWarnings().addAll(errors);
        }
        return result;
    }

    @Override
    @Transactional
    public ImportResult runImport(ImportRequest request, Reader reader) {
        validateImportFile(request);
        String targetTable = normalizeLower(request.getTargetTable());
        List<ImportRow> rows = parseRows(request, reader);
        ImportBatch batch = createBatch(request, rows.size());

        long successRows = 0L;
        List<String> errors = new ArrayList<>();
        for (ImportRow row : rows) {
            try {
                importRow(targetTable, row.values());
                successRows++;
            } catch (BusinessException exception) {
                String message = exception.getErrorCode().getMessage();
                errors.add(rowError(row.rowNo(), message));
                saveFailure(batch.getId(), row, null, message);
            } catch (RuntimeException exception) {
                String message = "行数据写入失败";
                errors.add(rowError(row.rowNo(), message));
                saveFailure(batch.getId(), row, null, message);
            }
        }

        long failedRows = rows.size() - successRows;
        String status = failedRows == 0 ? STATUS_SUCCESS : successRows == 0 ? STATUS_FAILED : STATUS_PARTIAL_SUCCESS;
        updateBatch(batch, status, rows.size(), successRows, failedRows, firstError(errors));

        ImportResult result = new ImportResult();
        result.setBatchName(batch.getBatchName());
        result.setTargetTable(targetTable);
        result.setStatus(status);
        result.setTotalRows(rows.size());
        result.setSuccessRows(successRows);
        result.setFailedRows(failedRows);
        result.setErrors(errors);
        if (successRows > 0) {
            invalidateImportedIndexes(targetTable);
        }
        return result;
    }

    private List<String> validateRows(String targetTable, List<ImportRow> rows) {
        List<String> errors = new ArrayList<>();
        for (ImportRow row : rows) {
            try {
                validateRequiredFields(targetTable, row.values());
            } catch (BusinessException exception) {
                errors.add(rowError(row.rowNo(), exception.getErrorCode().getMessage()));
            }
        }
        return errors;
    }

    private void importRow(String targetTable, Map<String, String> values) {
        validateRequiredFields(targetTable, values);
        switch (targetTable) {
            case "destination" -> importDestination(values);
            case "place" -> importPlace(values);
            case "facility" -> importFacility(values);
            case "food" -> importFood(values);
            case "map_node" -> importMapNode(values);
            case "map_edge" -> importMapEdge(values);
            default -> throw new BusinessException(ErrorCode.IMPORT_003);
        }
    }

    private void validateRequiredFields(String targetTable, Map<String, String> values) {
        switch (targetTable) {
            case "destination" -> requireFields(values, "name", "type");
            case "place" -> requireFields(values, "destination_id", "name", "place_type");
            case "facility" -> requireFields(values, "destination_id", "name", "facility_type");
            case "food" -> requireFields(values, "destination_id", "name");
            case "map_node" -> requireFields(values, "destination_id", "node_name", "node_type");
            case "map_edge" -> requireFields(values, "destination_id", "from_node_id", "to_node_id", "distance");
            default -> throw new BusinessException(ErrorCode.IMPORT_003);
        }
    }

    private void importDestination(Map<String, String> values) {
        Destination destination = new Destination();
        destination.setName(text(values, "name"));
        destination.setType(text(values, "type"));
        destination.setCategory(text(values, "category"));
        destination.setCity(text(values, "city"));
        destination.setDescription(text(values, "description"));
        destination.setHeatScore(decimalOrZero(values, "heat_score"));
        destination.setRatingScore(decimalOrZero(values, "rating_score"));
        destination.setTagJson(text(values, "tag_json"));
        destination.setCoverUrl(text(values, "cover_url"));
        destination.setStatus(intOrDefault(values, "status", ENABLED_STATUS));
        destination.setCreatedAt(LocalDateTime.now());
        destinationMapper.insert(destination);
    }

    private void importPlace(Map<String, String> values) {
        Long destinationId = longValue(values, "destination_id");
        requireDestination(destinationId);
        Place place = new Place();
        place.setDestinationId(destinationId);
        place.setName(text(values, "name"));
        place.setPlaceType(text(values, "place_type"));
        place.setDescription(text(values, "description"));
        place.setLng(decimal(values, "lng"));
        place.setLat(decimal(values, "lat"));
        place.setFloorInfo(text(values, "floor_info"));
        place.setHeatScore(decimalOrZero(values, "heat_score"));
        place.setRatingScore(decimalOrZero(values, "rating_score"));
        place.setOpenTimeRule(text(values, "open_time_rule"));
        place.setSuggestedDurationMin(intValue(values, "suggested_duration_min"));
        place.setCostLevel(intValue(values, "cost_level"));
        placeMapper.insert(place);
    }

    private void importFacility(Map<String, String> values) {
        Long destinationId = longValue(values, "destination_id");
        requireDestination(destinationId);
        Long placeId = longValue(values, "place_id");
        if (placeId != null && placeMapper.selectById(placeId) == null) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
        Facility facility = new Facility();
        facility.setDestinationId(destinationId);
        facility.setPlaceId(placeId);
        facility.setName(text(values, "name"));
        facility.setFacilityType(text(values, "facility_type"));
        facility.setDescription(text(values, "description"));
        facility.setAddress(text(values, "address"));
        facility.setTel(text(values, "tel"));
        facility.setCoverUrl(text(values, "cover_url"));
        facility.setLng(decimal(values, "lng"));
        facility.setLat(decimal(values, "lat"));
        facility.setStatus(intOrDefault(values, "status", ENABLED_STATUS));
        facilityMapper.insert(facility);
    }

    private void importFood(Map<String, String> values) {
        Long destinationId = longValue(values, "destination_id");
        requireDestination(destinationId);
        Long facilityId = longValue(values, "facility_id");
        if (facilityId != null && facilityMapper.selectById(facilityId) == null) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
        Food food = new Food();
        food.setDestinationId(destinationId);
        food.setFacilityId(facilityId);
        food.setName(text(values, "name"));
        food.setFoodType(text(values, "food_type"));
        food.setShopName(text(values, "shop_name"));
        food.setDescription(text(values, "description"));
        food.setHeatScore(decimalOrZero(values, "heat_score"));
        food.setRatingScore(decimalOrZero(values, "rating_score"));
        food.setAvgPrice(decimal(values, "avg_price"));
        food.setCoverUrl(text(values, "cover_url"));
        food.setLng(decimal(values, "lng"));
        food.setLat(decimal(values, "lat"));
        foodMapper.insert(food);
    }

    private void importMapNode(Map<String, String> values) {
        Long destinationId = longValue(values, "destination_id");
        requireDestination(destinationId);
        MapNode node = new MapNode();
        node.setDestinationId(destinationId);
        node.setNodeName(text(values, "node_name"));
        node.setNodeType(text(values, "node_type"));
        node.setRefId(longValue(values, "ref_id"));
        node.setLng(decimal(values, "lng"));
        node.setLat(decimal(values, "lat"));
        node.setFloorNo(intValue(values, "floor_no"));
        mapNodeMapper.insert(node);
    }

    private void importMapEdge(Map<String, String> values) {
        Long destinationId = longValue(values, "destination_id");
        requireDestination(destinationId);
        Long fromNodeId = longValue(values, "from_node_id");
        Long toNodeId = longValue(values, "to_node_id");
        MapNode fromNode = mapNodeMapper.selectById(fromNodeId);
        MapNode toNode = mapNodeMapper.selectById(toNodeId);
        if (fromNode == null || toNode == null
                || !destinationId.equals(fromNode.getDestinationId())
                || !destinationId.equals(toNode.getDestinationId())) {
            throw new BusinessException(ErrorCode.ROUTE_009);
        }
        MapEdge edge = new MapEdge();
        edge.setDestinationId(destinationId);
        edge.setFromNodeId(fromNodeId);
        edge.setToNodeId(toNodeId);
        edge.setDistance(decimalRequired(values, "distance"));
        edge.setIdealSpeed(decimal(values, "ideal_speed"));
        edge.setCrowdFactor(decimal(values, "crowd_factor"));
        edge.setTransportType(text(values, "transport_type"));
        edge.setEdgeType(text(values, "edge_type"));
        edge.setBidirectionalFlag(intOrDefault(values, "bidirectional_flag", 0));
        mapEdgeMapper.insert(edge);
    }

    private void invalidateImportedIndexes(String targetTable) {
        if ("destination".equals(targetTable)) {
            indexMaintenanceService.invalidate(IndexNamespace.DESTINATION_NAME);
        } else if ("food".equals(targetTable)) {
            indexMaintenanceService.invalidate(IndexNamespace.FOOD_NAME);
            indexMaintenanceService.invalidate(IndexNamespace.FOOD_SHOP_NAME);
        }
    }

    private List<ImportRow> parseRows(ImportRequest request, Reader reader) {
        if (reader == null) {
            throw new BusinessException(ErrorCode.IMPORT_001);
        }
        try {
            List<ImportRow> rows = switch (normalizeLower(request.getSourceType())) {
                case "csv" -> parseCsv(reader);
                case "json" -> parseJson(reader);
                default -> throw new BusinessException(ErrorCode.IMPORT_002);
            };
            if (rows.isEmpty()) {
                throw new BusinessException(ErrorCode.IMPORT_001);
            }
            if (rows.size() > MAX_IMPORT_ROWS) {
                throw new BusinessException(ErrorCode.IMPORT_004);
            }
            return rows;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.IMPORT_008);
        }
    }

    private List<ImportRow> parseCsv(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        String headerLine = bufferedReader.readLine();
        if (!StringUtils.hasText(headerLine)) {
            return List.of();
        }
        List<String> headers = parseCsvLine(headerLine);
        List<ImportRow> rows = new ArrayList<>();
        String line;
        int rowNo = 1;
        while ((line = bufferedReader.readLine()) != null) {
            rowNo++;
            if (!StringUtils.hasText(line)) {
                continue;
            }
            List<String> columns = parseCsvLine(line);
            Map<String, String> values = new LinkedHashMap<>();
            for (int index = 0; index < headers.size(); index++) {
                String header = normalizeCsvHeader(headers.get(index), index);
                String value = index < columns.size() ? normalize(columns.get(index)) : null;
                values.put(header, value);
            }
            rows.add(new ImportRow(rowNo, values, line));
        }
        return rows;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char item = line.charAt(index);
            if (item == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (item == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(item);
            }
        }
        values.add(current.toString());
        return values;
    }

    private List<ImportRow> parseJson(Reader reader) throws IOException {
        List<Map<String, Object>> rawRows = objectMapper.readValue(reader, new TypeReference<>() {});
        List<ImportRow> rows = new ArrayList<>();
        for (int index = 0; index < rawRows.size(); index++) {
            Map<String, String> values = new LinkedHashMap<>();
            rawRows.get(index).forEach((key, value) -> values.put(normalizeLower(key), value == null ? null : String.valueOf(value)));
            rows.add(new ImportRow(index + 1, values, rawJson(rawRows.get(index))));
        }
        return rows;
    }

    private ImportBatch createBatch(ImportRequest request, long totalRows) {
        LocalDateTime now = LocalDateTime.now();
        ImportBatch batch = new ImportBatch();
        batch.setBatchName(batchName(request));
        batch.setTargetTable(normalizeLower(request.getTargetTable()));
        batch.setSourceType(normalizeLower(request.getSourceType()));
        batch.setFileName(normalize(request.getFileName()));
        batch.setFileSize(request.getFileSize());
        batch.setStatus("RUNNING");
        batch.setTotalRows(totalRows);
        batch.setSuccessRows(0L);
        batch.setFailedRows(0L);
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        importBatchMapper.insert(batch);
        return batch;
    }

    private void updateBatch(ImportBatch batch, String status, long totalRows, long successRows, long failedRows, String errorMessage) {
        batch.setStatus(status);
        batch.setTotalRows(totalRows);
        batch.setSuccessRows(successRows);
        batch.setFailedRows(failedRows);
        batch.setErrorMessage(errorMessage);
        batch.setUpdatedAt(LocalDateTime.now());
        importBatchMapper.updateById(batch);
    }

    private void saveFailure(Long batchId, ImportRow row, String fieldName, String message) {
        ImportFailure failure = new ImportFailure();
        failure.setBatchId(batchId);
        failure.setRowNo(row.rowNo());
        failure.setFieldName(fieldName);
        failure.setErrorMessage(message);
        failure.setRawDataJson(rawJson(row.values()));
        failure.setCreatedAt(LocalDateTime.now());
        importFailureMapper.insert(failure);
    }

    private void requireDestination(Long destinationId) {
        if (destinationId == null || destinationMapper.selectById(destinationId) == null) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
    }

    private void requireFields(Map<String, String> values, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (!StringUtils.hasText(values.get(fieldName))) {
                throw new BusinessException(ErrorCode.IMPORT_003);
            }
        }
    }

    private String text(Map<String, String> values, String fieldName) {
        return normalize(values.get(fieldName));
    }

    private Long longValue(Map<String, String> values, String fieldName) {
        String value = text(values, fieldName);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.IMPORT_004);
        }
    }

    private Integer intValue(Map<String, String> values, String fieldName) {
        String value = text(values, fieldName);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.IMPORT_004);
        }
    }

    private Integer intOrDefault(Map<String, String> values, String fieldName, int defaultValue) {
        Integer value = intValue(values, fieldName);
        return value == null ? defaultValue : value;
    }

    private BigDecimal decimal(Map<String, String> values, String fieldName) {
        String value = text(values, fieldName);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.IMPORT_004);
        }
    }

    private BigDecimal decimalRequired(Map<String, String> values, String fieldName) {
        BigDecimal value = decimal(values, fieldName);
        if (value == null) {
            throw new BusinessException(ErrorCode.IMPORT_003);
        }
        return value;
    }

    private BigDecimal decimalOrZero(Map<String, String> values, String fieldName) {
        BigDecimal value = decimal(values, fieldName);
        return value == null ? BigDecimal.ZERO : value;
    }

    private String rawJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String batchName(ImportRequest request) {
        return normalizeLower(request.getTargetTable()) + "_" + LocalDateTime.now().format(BATCH_TIME_FORMAT);
    }

    private String rowError(int rowNo, String message) {
        return "第 " + rowNo + " 行：" + message;
    }

    private String firstError(List<String> errors) {
        return errors.isEmpty() ? null : errors.get(0);
    }

    private String normalizeLower(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeCsvHeader(String value, int index) {
        String header = value;
        if (index == 0 && header != null && !header.isEmpty() && header.charAt(0) == '\uFEFF') {
            header = header.substring(1);
        }
        return normalizeLower(header);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private record ImportRow(int rowNo, Map<String, String> values, String rawData) {}
}
