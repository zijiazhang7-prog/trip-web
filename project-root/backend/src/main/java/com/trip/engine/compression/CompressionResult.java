package com.trip.engine.compression;

/**
 * Huffman 压缩结果及基础统计信息。
 */
public record CompressionResult(
        byte[] data,
        int originalByteLength,
        int compressedByteLength,
        long encodedBitLength) {

    public CompressionResult {
        data = data == null ? new byte[0] : data.clone();
    }

    @Override
    public byte[] data() {
        return data.clone();
    }
}
