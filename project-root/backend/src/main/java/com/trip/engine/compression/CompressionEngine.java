package com.trip.engine.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.zip.CRC32;
import org.springframework.stereotype.Component;

/**
 * 基于 Unicode 码点和 Huffman 树的日记正文无损压缩引擎。
 */
@Component
public class CompressionEngine {

    private static final int MAGIC = 0x54484631;
    private static final int VERSION = 1;
    private static final int MAX_ORIGINAL_BYTES = 10 * 1024 * 1024;
    private static final int MAX_SYMBOLS = Character.MAX_CODE_POINT + 1;

    /**
     * 将文本编码为包含频次表、位流长度和校验值的自描述二进制包。
     *
     * <p>数据结构：频次 Hash/Tree Map、优先队列和 Huffman 二叉树。
     * 时间复杂度：O(n + k log k)，空间复杂度：O(k + b)。
     */
    public CompressionResult compress(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text must not be null");
        }

        byte[] originalBytes = text.getBytes(StandardCharsets.UTF_8);
        if (originalBytes.length > MAX_ORIGINAL_BYTES) {
            throw new IllegalArgumentException("Text is too large to compress");
        }

        int[] codePoints = text.codePoints().toArray();
        Map<Integer, Long> frequencies = frequencies(codePoints);
        HuffmanNode root = buildTree(frequencies);
        Map<Integer, BitCode> codes = new HashMap<>();
        if (root != null) {
            buildCodes(root, new ArrayList<>(), codes);
        }

        BitWriter writer = new BitWriter();
        for (int codePoint : codePoints) {
            writer.write(codes.get(codePoint));
        }
        byte[] encodedBytes = writer.toByteArray();
        byte[] packageBytes = writePackage(
                originalBytes.length,
                codePoints.length,
                frequencies,
                writer.bitLength(),
                encodedBytes,
                crc32(originalBytes));
        return new CompressionResult(
                packageBytes,
                originalBytes.length,
                packageBytes.length,
                writer.bitLength());
    }

    /**
     * 解码自描述 Huffman 二进制包，并校验长度、频次和 CRC32。
     */
    public String decompress(byte[] compressedData) {
        if (compressedData == null || compressedData.length == 0) {
            throw new IllegalArgumentException("Compressed data must not be empty");
        }

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(compressedData))) {
            Header header = readHeader(input);
            HuffmanNode root = buildTree(header.frequencies());
            int[] decoded = decode(root, header.encodedBytes(), header.bitLength(), header.codePointCount());
            validateFrequencies(decoded, header.frequencies());

            String text = new String(decoded, 0, decoded.length);
            byte[] originalBytes = text.getBytes(StandardCharsets.UTF_8);
            if (originalBytes.length != header.originalByteLength()
                    || crc32(originalBytes) != header.crc32()) {
                throw new IllegalArgumentException("Compressed data integrity check failed");
            }
            return text;
        } catch (EOFException exception) {
            throw new IllegalArgumentException("Compressed data is truncated", exception);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Compressed data cannot be decoded", exception);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Compressed data contains invalid frequencies", exception);
        }
    }

    private Map<Integer, Long> frequencies(int[] codePoints) {
        Map<Integer, Long> frequencies = new TreeMap<>();
        for (int codePoint : codePoints) {
            frequencies.merge(codePoint, 1L, Long::sum);
        }
        return frequencies;
    }

    private HuffmanNode buildTree(Map<Integer, Long> frequencies) {
        PriorityQueue<HuffmanNode> queue = new PriorityQueue<>(Comparator
                .comparingLong(HuffmanNode::frequency)
                .thenComparingInt(HuffmanNode::minCodePoint));
        for (Map.Entry<Integer, Long> entry : frequencies.entrySet()) {
            if (!Character.isValidCodePoint(entry.getKey()) || entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("Compressed data contains an invalid symbol");
            }
            queue.add(HuffmanNode.leaf(entry.getKey(), entry.getValue()));
        }
        while (queue.size() > 1) {
            queue.add(HuffmanNode.parent(queue.remove(), queue.remove()));
        }
        return queue.poll();
    }

    private void buildCodes(HuffmanNode node, List<Boolean> path, Map<Integer, BitCode> codes) {
        if (node.isLeaf()) {
            if (path.isEmpty()) {
                path.add(false);
                codes.put(node.codePoint(), BitCode.from(path));
                path.remove(path.size() - 1);
            } else {
                codes.put(node.codePoint(), BitCode.from(path));
            }
            return;
        }

        path.add(false);
        buildCodes(node.left(), path, codes);
        path.remove(path.size() - 1);
        path.add(true);
        buildCodes(node.right(), path, codes);
        path.remove(path.size() - 1);
    }

    private byte[] writePackage(
            int originalByteLength,
            int codePointCount,
            Map<Integer, Long> frequencies,
            long bitLength,
            byte[] encodedBytes,
            long checksum) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(buffer)) {
                output.writeInt(MAGIC);
                output.writeByte(VERSION);
                output.writeInt(originalByteLength);
                output.writeInt(codePointCount);
                output.writeInt(frequencies.size());
                for (Map.Entry<Integer, Long> entry : frequencies.entrySet()) {
                    output.writeInt(entry.getKey());
                    output.writeLong(entry.getValue());
                }
                output.writeLong(bitLength);
                output.writeInt(encodedBytes.length);
                output.write(encodedBytes);
                output.writeLong(checksum);
            }
            return buffer.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build compressed data", exception);
        }
    }

    private Header readHeader(DataInputStream input) throws IOException {
        if (input.readInt() != MAGIC) {
            throw new IllegalArgumentException("Compressed data magic does not match");
        }
        if (input.readUnsignedByte() != VERSION) {
            throw new IllegalArgumentException("Compressed data version is not supported");
        }

        int originalByteLength = input.readInt();
        int codePointCount = input.readInt();
        int symbolCount = input.readInt();
        if (originalByteLength < 0
                || originalByteLength > MAX_ORIGINAL_BYTES
                || codePointCount < 0
                || symbolCount < 0
                || symbolCount > MAX_SYMBOLS
                || symbolCount > codePointCount) {
            throw new IllegalArgumentException("Compressed data header is invalid");
        }

        Map<Integer, Long> frequencies = new TreeMap<>();
        long frequencyTotal = 0;
        for (int index = 0; index < symbolCount; index++) {
            int codePoint = input.readInt();
            long frequency = input.readLong();
            if (!Character.isValidCodePoint(codePoint)
                    || frequency <= 0
                    || frequencies.put(codePoint, frequency) != null) {
                throw new IllegalArgumentException("Compressed data frequency table is invalid");
            }
            frequencyTotal = Math.addExact(frequencyTotal, frequency);
        }
        if (frequencyTotal != codePointCount) {
            throw new IllegalArgumentException("Compressed data frequency total does not match");
        }

        long bitLength = input.readLong();
        int encodedByteLength = input.readInt();
        long expectedByteLength = (bitLength + 7L) / 8L;
        if (bitLength < 0
                || encodedByteLength < 0
                || encodedByteLength != expectedByteLength
                || encodedByteLength > input.available() - Long.BYTES) {
            throw new IllegalArgumentException("Compressed data bit stream length is invalid");
        }
        byte[] encodedBytes = input.readNBytes(encodedByteLength);
        long checksum = input.readLong();
        if (input.available() != 0) {
            throw new IllegalArgumentException("Compressed data contains trailing bytes");
        }
        return new Header(
                originalByteLength,
                codePointCount,
                frequencies,
                bitLength,
                encodedBytes,
                checksum);
    }

    private int[] decode(HuffmanNode root, byte[] encodedBytes, long bitLength, int codePointCount) {
        if (codePointCount == 0) {
            if (root != null || bitLength != 0) {
                throw new IllegalArgumentException("Compressed empty text is invalid");
            }
            return new int[0];
        }
        if (root == null || bitLength == 0) {
            throw new IllegalArgumentException("Compressed data has no Huffman tree");
        }

        int[] decoded = new int[codePointCount];
        if (root.isLeaf()) {
            if (bitLength != codePointCount) {
                throw new IllegalArgumentException("Single-symbol bit length does not match");
            }
            for (int index = 0; index < codePointCount; index++) {
                if (bit(encodedBytes, index)) {
                    throw new IllegalArgumentException("Single-symbol stream contains an invalid bit");
                }
                decoded[index] = root.codePoint();
            }
            return decoded;
        }

        HuffmanNode current = root;
        int decodedCount = 0;
        for (long bitIndex = 0; bitIndex < bitLength; bitIndex++) {
            current = bit(encodedBytes, bitIndex) ? current.right() : current.left();
            if (current == null) {
                throw new IllegalArgumentException("Compressed data contains an invalid code");
            }
            if (current.isLeaf()) {
                if (decodedCount >= decoded.length) {
                    throw new IllegalArgumentException("Compressed data decodes too many symbols");
                }
                decoded[decodedCount++] = current.codePoint();
                current = root;
            }
        }
        if (current != root || decodedCount != codePointCount) {
            throw new IllegalArgumentException("Compressed data ends with an incomplete code");
        }
        return decoded;
    }

    private void validateFrequencies(int[] decoded, Map<Integer, Long> expected) {
        if (!frequencies(decoded).equals(expected)) {
            throw new IllegalArgumentException("Compressed data frequency table does not match content");
        }
    }

    private boolean bit(byte[] data, long index) {
        int byteIndex = Math.toIntExact(index / 8L);
        int bitOffset = 7 - (int) (index % 8L);
        return ((data[byteIndex] >>> bitOffset) & 1) == 1;
    }

    private long crc32(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    private record BitCode(boolean[] bits) {

        private static BitCode from(List<Boolean> values) {
            boolean[] bits = new boolean[values.size()];
            for (int index = 0; index < values.size(); index++) {
                bits[index] = values.get(index);
            }
            return new BitCode(bits);
        }
    }

    private static final class BitWriter {

        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private int currentByte;
        private int usedBits;
        private long bitLength;

        private void write(BitCode code) {
            if (code == null) {
                throw new IllegalArgumentException("Missing Huffman code");
            }
            for (boolean bit : code.bits()) {
                currentByte = (currentByte << 1) | (bit ? 1 : 0);
                usedBits++;
                bitLength++;
                if (usedBits == Byte.SIZE) {
                    output.write(currentByte);
                    currentByte = 0;
                    usedBits = 0;
                }
            }
        }

        private byte[] toByteArray() {
            if (usedBits > 0) {
                output.write(currentByte << (Byte.SIZE - usedBits));
                currentByte = 0;
                usedBits = 0;
            }
            return output.toByteArray();
        }

        private long bitLength() {
            return bitLength;
        }
    }

    private record Header(
            int originalByteLength,
            int codePointCount,
            Map<Integer, Long> frequencies,
            long bitLength,
            byte[] encodedBytes,
            long crc32) {
    }
}
