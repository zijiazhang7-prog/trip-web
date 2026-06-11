package com.trip.engine.compression;

/**
 * Huffman 树节点。内部节点的 codePoint 为 -1。
 */
record HuffmanNode(
        int codePoint,
        long frequency,
        int minCodePoint,
        HuffmanNode left,
        HuffmanNode right) {

    static HuffmanNode leaf(int codePoint, long frequency) {
        return new HuffmanNode(codePoint, frequency, codePoint, null, null);
    }

    static HuffmanNode parent(HuffmanNode left, HuffmanNode right) {
        return new HuffmanNode(
                -1,
                Math.addExact(left.frequency(), right.frequency()),
                Math.min(left.minCodePoint(), right.minCodePoint()),
                left,
                right);
    }

    boolean isLeaf() {
        return left == null && right == null;
    }
}
