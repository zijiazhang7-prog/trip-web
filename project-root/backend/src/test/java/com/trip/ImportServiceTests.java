package com.trip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.common.ErrorCode;
import com.trip.dto.imports.ImportPreviewResult;
import com.trip.dto.imports.ImportRequest;
import com.trip.dto.imports.ImportResult;
import com.trip.entity.Destination;
import com.trip.entity.ImportBatch;
import com.trip.exception.BusinessException;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.FacilityMapper;
import com.trip.mapper.FoodMapper;
import com.trip.mapper.ImportBatchMapper;
import com.trip.mapper.ImportFailureMapper;
import com.trip.mapper.MapEdgeMapper;
import com.trip.mapper.MapNodeMapper;
import com.trip.mapper.PlaceMapper;
import com.trip.service.impl.ImportServiceImpl;
import java.io.StringReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportServiceTests {

    private DestinationMapper destinationMapper;
    private ImportBatchMapper importBatchMapper;
    private ImportServiceImpl importService;

    @BeforeEach
    void setUp() {
        destinationMapper = mock(DestinationMapper.class);
        PlaceMapper placeMapper = mock(PlaceMapper.class);
        FacilityMapper facilityMapper = mock(FacilityMapper.class);
        FoodMapper foodMapper = mock(FoodMapper.class);
        MapNodeMapper mapNodeMapper = mock(MapNodeMapper.class);
        MapEdgeMapper mapEdgeMapper = mock(MapEdgeMapper.class);
        importBatchMapper = mock(ImportBatchMapper.class);
        ImportFailureMapper importFailureMapper = mock(ImportFailureMapper.class);
        when(importBatchMapper.insert(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch batch = invocation.getArgument(0);
            batch.setId(1L);
            return 1;
        });
        importService = new ImportServiceImpl(
                new ObjectMapper(),
                destinationMapper,
                placeMapper,
                facilityMapper,
                foodMapper,
                mapNodeMapper,
                mapEdgeMapper,
                importBatchMapper,
                importFailureMapper);
    }

    @Test
    void validateImportFileShouldAcceptSupportedCsvFile() {
        assertDoesNotThrow(() -> importService.validateImportFile(request("destination", "csv", "destinations.csv", 1024L)));
    }

    @Test
    void validateImportFileShouldAcceptSupportedJsonFile() {
        assertDoesNotThrow(() -> importService.validateImportFile(request("map_node", "json", "map_nodes.json", 2048L)));
    }

    @Test
    void validateImportFileShouldRejectSqlFile() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> importService.validateImportFile(request("route_history", "sql", "route_history.sql", 4096L)));

        assertEquals(ErrorCode.IMPORT_003, exception.getErrorCode());
    }

    @Test
    void validateImportFileShouldRejectNullRequest() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> importService.validateImportFile(null));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
    }

    @Test
    void validateImportFileShouldRejectEmptyFileName() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> importService.validateImportFile(request("destination", "csv", " ", 1024L)));

        assertEquals(ErrorCode.IMPORT_001, exception.getErrorCode());
    }

    @Test
    void validateImportFileShouldRejectUnsupportedSourceType() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> importService.validateImportFile(request("destination", "xlsx", "destinations.xlsx", 1024L)));

        assertEquals(ErrorCode.IMPORT_002, exception.getErrorCode());
    }

    @Test
    void validateImportFileShouldRejectMismatchedExtension() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> importService.validateImportFile(request("destination", "csv", "destinations.json", 1024L)));

        assertEquals(ErrorCode.IMPORT_002, exception.getErrorCode());
    }

    @Test
    void validateImportFileShouldRejectUnsupportedTargetTable() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> importService.validateImportFile(request("user", "csv", "users.csv", 1024L)));

        assertEquals(ErrorCode.IMPORT_003, exception.getErrorCode());
    }

    @Test
    void validateImportFileShouldRejectInvalidFileSize() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> importService.validateImportFile(request("destination", "csv", "destinations.csv", 0L)));

        assertEquals(ErrorCode.IMPORT_004, exception.getErrorCode());
    }

    @Test
    void validateImportFileShouldRejectTooLargeFile() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> importService.validateImportFile(request("destination", "csv", "destinations.csv", 21L * 1024 * 1024)));

        assertEquals(ErrorCode.IMPORT_004, exception.getErrorCode());
    }

    @Test
    void previewImportShouldParseCsvRows() {
        ImportPreviewResult result = importService.previewImport(
                request(" destination ", " CSV ", "destinations.csv", 1024L),
                reader("name,type,city\n测试目的地,campus,北京\n"));

        assertTrue(result.getBatchName().startsWith("destination_"));
        assertEquals("destination", result.getTargetTable());
        assertEquals("csv", result.getSourceType());
        assertEquals("destinations.csv", result.getFileName());
        assertEquals("PREVIEW_ONLY", result.getStatus());
        assertEquals(1L, result.getTotalRows());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void previewImportShouldParseCsvHeaderWithUtf8Bom() {
        ImportPreviewResult result = importService.previewImport(
                request("destination", "csv", "destinations.csv", 1024L),
                reader("\uFEFFname,type,city\n测试目的地,campus,北京\n"));

        assertEquals(1L, result.getTotalRows());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void previewImportShouldReportMissingRequiredFields() {
        ImportPreviewResult result = importService.previewImport(
                request("destination", "json", "destinations.json", 1024L),
                reader("[{\"name\":\"测试目的地\"}]"));

        assertEquals(1L, result.getTotalRows());
        assertFalse(result.getWarnings().isEmpty());
    }

    @Test
    void runImportShouldInsertDestinationRowsAndBatchSummary() {
        ImportResult result = importService.runImport(
                request("destination", "csv", "destinations.csv", 1024L),
                reader("name,type,city\n测试目的地,campus,北京\n"));

        assertTrue(result.getBatchName().startsWith("destination_"));
        assertEquals("destination", result.getTargetTable());
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(1L, result.getTotalRows());
        assertEquals(1L, result.getSuccessRows());
        assertEquals(0L, result.getFailedRows());
        assertTrue(result.getErrors().isEmpty());
        verify(destinationMapper).insert(any(Destination.class));
        verify(importBatchMapper).insert(any(ImportBatch.class));
        verify(importBatchMapper).updateById(any(ImportBatch.class));
    }

    private StringReader reader(String value) {
        return new StringReader(value);
    }

    private ImportRequest request(String targetTable, String sourceType, String fileName, Long fileSize) {
        ImportRequest request = new ImportRequest();
        request.setTargetTable(targetTable);
        request.setSourceType(sourceType);
        request.setFileName(fileName);
        request.setFileSize(fileSize);
        return request;
    }
}
