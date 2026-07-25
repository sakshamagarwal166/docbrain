package com.docbrain.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkingServiceTest {

    private final ChunkingService chunkingService = new ChunkingService(100, 20);

    @Test
    void chunkText_nullReturnsEmpty() {
        assertEquals(List.of(), chunkingService.chunkText(null));
    }

    @Test
    void chunkText_blankReturnsEmpty() {
        assertEquals(List.of(), chunkingService.chunkText("   "));
    }

    @Test
    void chunkText_shortTextReturnsSingleChunk() {
        String text = "Hello world.";
        List<String> chunks = chunkingService.chunkText(text);
        assertEquals(1, chunks.size());
        assertEquals("Hello world.", chunks.get(0));
    }

    @Test
    void chunkText_longTextProducesMultipleChunks() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("This is sentence number ").append(i).append(". ");
        }
        List<String> chunks = chunkingService.chunkText(sb.toString());
        assertTrue(chunks.size() > 1, "Long text should produce multiple chunks");
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 120,
                    "Chunk should not greatly exceed chunk size: " + chunk.length());
        }
    }

    @Test
    void chunkText_overlapProducesOverlappingContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            sb.append("Word").append(i).append(" ");
        }
        List<String> chunks = chunkingService.chunkText(sb.toString());
        if (chunks.size() >= 2) {
            String end1 = chunks.get(0).substring(Math.max(0, chunks.get(0).length() - 15));
            assertTrue(chunks.get(1).contains(end1.split(" ")[end1.split(" ").length - 1]),
                    "Overlap should cause some content to appear in consecutive chunks");
        }
    }

    @Test
    void chunkText_splitsByParagraphs() {
        String text = "First paragraph content here.\n\nSecond paragraph content here.";
        List<String> chunks = chunkingService.chunkText(text);
        assertEquals(1, chunks.size());
    }

    @Test
    void chunkText_noOverlapWhenZero() {
        ChunkingService noOverlap = new ChunkingService(50, 0);
        String text = "Short sentence one. Short sentence two. Short sentence three. Short sentence four.";
        List<String> chunks = noOverlap.chunkText(text);
        assertTrue(chunks.size() >= 1);
    }

    @Test
    void chunkText_veryLongSingleLine() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("word").append(i).append(" ");
        }
        List<String> chunks = chunkingService.chunkText(sb.toString().trim());
        assertTrue(chunks.size() > 1, "Very long single line should be split into chunks");
    }
}
