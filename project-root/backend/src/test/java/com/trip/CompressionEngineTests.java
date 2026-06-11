package com.trip;

import com.trip.engine.compression.CompressionEngine;
import com.trip.engine.compression.CompressionResult;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompressionEngineTests {

    private final CompressionEngine compressionEngine = new CompressionEngine();

    @Test
    void shouldRoundTripAsciiChineseAndUnicodeText() {
        for (String text : new String[] {
                "huffman coding",
                "今天去了图书馆，今天很开心。",
                "旅行😀🚄𠀀结束"
        }) {
            CompressionResult result = compressionEngine.compress(text);

            assertEquals(text, compressionEngine.decompress(result.data()));
            assertEquals(text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, result.originalByteLength());
            assertEquals(result.data().length, result.compressedByteLength());
        }
    }

    @Test
    void shouldSupportEmptyAndSingleSymbolText() {
        assertEquals("", compressionEngine.decompress(compressionEngine.compress("").data()));
        assertEquals("人人人人人", compressionEngine.decompress(compressionEngine.compress("人人人人人").data()));
    }

    @Test
    void shouldProduceDeterministicPackage() {
        String text = "相同频次的字符也必须稳定编码 abcd dcba";

        byte[] first = compressionEngine.compress(text).data();
        byte[] second = compressionEngine.compress(text).data();

        assertArrayEquals(first, second);
    }

    @Test
    void shouldHandleMaximumDiaryLength() {
        String text = "旅行见闻".repeat(2500);

        CompressionResult result = compressionEngine.compress(text);

        assertEquals(text, compressionEngine.decompress(result.data()));
        assertTrue(result.encodedBitLength() > 0);
    }

    @Test
    void shouldReduceRepeatedLongTextPayload() {
        String text = "aaaaab".repeat(2000);

        CompressionResult result = compressionEngine.compress(text);

        assertTrue(result.compressedByteLength() < result.originalByteLength());
    }

    @Test
    void shouldRejectNullOrEmptyCompressedInput() {
        assertThrows(IllegalArgumentException.class, () -> compressionEngine.compress(null));
        assertThrows(IllegalArgumentException.class, () -> compressionEngine.decompress(null));
        assertThrows(IllegalArgumentException.class, () -> compressionEngine.decompress(new byte[0]));
    }

    @Test
    void shouldRejectInvalidMagicVersionTruncationAndChecksum() {
        byte[] valid = compressionEngine.compress("完整性校验文本").data();

        byte[] invalidMagic = valid.clone();
        invalidMagic[0] ^= 1;
        assertThrows(IllegalArgumentException.class, () -> compressionEngine.decompress(invalidMagic));

        byte[] invalidVersion = valid.clone();
        invalidVersion[4] = 99;
        assertThrows(IllegalArgumentException.class, () -> compressionEngine.decompress(invalidVersion));

        byte[] truncated = Arrays.copyOf(valid, valid.length - 1);
        assertThrows(IllegalArgumentException.class, () -> compressionEngine.decompress(truncated));

        byte[] invalidChecksum = valid.clone();
        invalidChecksum[invalidChecksum.length - 1] ^= 1;
        assertThrows(IllegalArgumentException.class, () -> compressionEngine.decompress(invalidChecksum));
    }
}
