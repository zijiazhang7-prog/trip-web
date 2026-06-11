package com.trip;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.trip.common.ErrorCode;
import com.trip.dto.query.FacilityQuery;
import com.trip.dto.request.NearbyFacilityQuery;
import com.trip.entity.Facility;
import com.trip.entity.MapNode;
import com.trip.exception.BusinessException;
import com.trip.mapper.MapNodeMapper;
import com.trip.service.MapService;
import com.trip.service.QueryService;
import com.trip.service.impl.FacilityServiceImpl;
import com.trip.service.impl.RankServiceImpl;
import com.trip.vo.response.NearbyFacilityVO;
import com.trip.vo.response.PageResultVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class FacilityServiceTests {

    private final QueryService queryService = mock(QueryService.class);
    private final MapService mapService = mock(MapService.class);
    private final MapNodeMapper mapNodeMapper = mock(MapNodeMapper.class);
    private final FacilityServiceImpl facilityService = new FacilityServiceImpl(
            queryService,
            new RankServiceImpl(),
            mapService,
            mapNodeMapper);

    @Test
    void nearbyShouldSortByReachableDistance() {
        when(queryService.queryFacilities(any(FacilityQuery.class))).thenReturn(List.of(
                facility(1L, "直线近但绕路远", "toilet"),
                facility(2L, "图上最近", "toilet"),
                facility(3L, "图上第二近", "toilet")));
        when(mapService.shortestDistances(101L, 10L)).thenReturn(Map.of(
                201L, new BigDecimal("500.00"),
                202L, new BigDecimal("120.00"),
                203L, new BigDecimal("300.00")));
        when(mapNodeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                node(201L, 1L),
                node(202L, 2L),
                node(203L, 3L)));

        PageResultVO<NearbyFacilityVO> result = facilityService.findNearbyFacilities(baseQuery());

        assertEquals(List.of(2L, 3L, 1L), result.getList().stream().map(NearbyFacilityVO::getId).toList());
        assertEquals(new BigDecimal("120.00"), result.getList().get(0).getReachableDistance());
        assertEquals(10L, result.getList().get(0).getSourceNodeId());
        assertEquals(202L, result.getList().get(0).getTargetNodeId());
        assertEquals("校园服务点", result.getList().get(0).getAddress());
        assertEquals("010-12345678", result.getList().get(0).getTel());
        assertEquals("/files/facility/2.jpg", result.getList().get(0).getCoverUrl());
    }

    @Test
    void nearbyShouldPassFacilityTypeToQueryService() {
        when(queryService.queryFacilities(any(FacilityQuery.class))).thenReturn(List.of());

        NearbyFacilityQuery query = baseQuery();
        query.setFacilityType(" cafe ");

        facilityService.findNearbyFacilities(query);

        ArgumentCaptor<FacilityQuery> captor = ArgumentCaptor.forClass(FacilityQuery.class);
        verify(queryService).queryFacilities(captor.capture());
        assertEquals(101L, captor.getValue().getDestinationId());
        assertEquals("cafe", captor.getValue().getFacilityType());
        verifyNoInteractions(mapService);
    }

    @Test
    void nearbyShouldSkipMissingMappingAndUnreachableFacilities() {
        when(queryService.queryFacilities(any(FacilityQuery.class))).thenReturn(List.of(
                facility(1L, "有映射且可达", "shop"),
                facility(2L, "无节点映射", "shop"),
                facility(3L, "有映射但不可达", "shop")));
        when(mapService.shortestDistances(101L, 10L)).thenReturn(Map.of(201L, new BigDecimal("20.00")));
        when(mapNodeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                node(201L, 1L),
                node(203L, 3L)));

        PageResultVO<NearbyFacilityVO> result = facilityService.findNearbyFacilities(baseQuery());

        assertEquals(1, result.getList().size());
        assertEquals(1L, result.getList().get(0).getId());
        assertEquals(1, result.getTotal());
    }

    @Test
    void nearbyShouldFilterByRadius() {
        when(queryService.queryFacilities(any(FacilityQuery.class))).thenReturn(List.of(
                facility(1L, "范围内", "toilet"),
                facility(2L, "范围外", "toilet")));
        when(mapService.shortestDistances(101L, 10L)).thenReturn(Map.of(
                201L, new BigDecimal("80.00"),
                202L, new BigDecimal("180.00")));
        when(mapNodeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                node(201L, 1L),
                node(202L, 2L)));

        NearbyFacilityQuery query = baseQuery();
        query.setRadius(100);

        PageResultVO<NearbyFacilityVO> result = facilityService.findNearbyFacilities(query);

        assertEquals(List.of(1L), result.getList().stream().map(NearbyFacilityVO::getId).toList());
    }

    @Test
    void nearbyShouldPageAfterDistanceSorting() {
        when(queryService.queryFacilities(any(FacilityQuery.class))).thenReturn(List.of(
                facility(1L, "A", "toilet"),
                facility(2L, "B", "toilet"),
                facility(3L, "C", "toilet")));
        when(mapService.shortestDistances(101L, 10L)).thenReturn(Map.of(
                201L, new BigDecimal("30.00"),
                202L, new BigDecimal("10.00"),
                203L, new BigDecimal("20.00")));
        when(mapNodeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                node(201L, 1L),
                node(202L, 2L),
                node(203L, 3L)));

        NearbyFacilityQuery query = baseQuery();
        query.setPageNum(2);
        query.setPageSize(1);

        PageResultVO<NearbyFacilityVO> result = facilityService.findNearbyFacilities(query);

        assertEquals(List.of(3L), result.getList().stream().map(NearbyFacilityVO::getId).toList());
        assertEquals(3, result.getTotal());
        assertEquals(3, result.getPages());
    }

    @Test
    void nearbyShouldRejectUnsupportedSortBy() {
        NearbyFacilityQuery query = baseQuery();
        query.setSortBy("time");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> facilityService.findNearbyFacilities(query));

        assertEquals(ErrorCode.COMMON_008, exception.getErrorCode());
        verifyNoInteractions(queryService);
    }

    @Test
    void nearbyShouldPropagateMapServiceError() {
        when(queryService.queryFacilities(any(FacilityQuery.class))).thenReturn(List.of(facility(1L, "厕所", "toilet")));
        when(mapService.shortestDistances(101L, 10L)).thenThrow(new BusinessException(ErrorCode.ROUTE_001));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> facilityService.findNearbyFacilities(baseQuery()));

        assertEquals(ErrorCode.ROUTE_001, exception.getErrorCode());
    }

    private NearbyFacilityQuery baseQuery() {
        NearbyFacilityQuery query = new NearbyFacilityQuery();
        query.setDestinationId(101L);
        query.setSourceNodeId(10L);
        return query;
    }

    private Facility facility(Long id, String name, String facilityType) {
        Facility facility = new Facility();
        facility.setId(id);
        facility.setDestinationId(101L);
        facility.setName(name);
        facility.setFacilityType(facilityType);
        facility.setDescription("测试设施");
        facility.setAddress("校园服务点");
        facility.setTel("010-12345678");
        facility.setCoverUrl("/files/facility/" + id + ".jpg");
        facility.setLng(new BigDecimal("116.123456"));
        facility.setLat(new BigDecimal("40.123456"));
        facility.setStatus(1);
        return facility;
    }

    private MapNode node(Long nodeId, Long facilityId) {
        MapNode node = new MapNode();
        node.setId(nodeId);
        node.setDestinationId(101L);
        node.setNodeType("facility");
        node.setRefId(facilityId);
        node.setNodeName("设施节点");
        return node;
    }
}
