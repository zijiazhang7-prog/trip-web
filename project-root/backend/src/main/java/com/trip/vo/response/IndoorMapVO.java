package com.trip.vo.response;

import java.util.List;

/**
 * 单栋建筑的室内楼层图数据。
 */
public class IndoorMapVO {

    private IndoorBuildingVO building;
    private List<IndoorNodeVO> nodes;
    private List<IndoorEdgeVO> edges;

    public IndoorBuildingVO getBuilding() {
        return building;
    }

    public void setBuilding(IndoorBuildingVO building) {
        this.building = building;
    }

    public List<IndoorNodeVO> getNodes() {
        return nodes;
    }

    public void setNodes(List<IndoorNodeVO> nodes) {
        this.nodes = nodes;
    }

    public List<IndoorEdgeVO> getEdges() {
        return edges;
    }

    public void setEdges(List<IndoorEdgeVO> edges) {
        this.edges = edges;
    }
}
