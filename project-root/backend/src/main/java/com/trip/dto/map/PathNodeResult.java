package com.trip.dto.map;

/**
 * 最短路径中的节点结果，供 Route 和 Facility 后续组装业务 VO 使用。
 */
public class PathNodeResult {

    private Long nodeId;

    private String nodeName;

    public PathNodeResult(Long nodeId, String nodeName) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
}
