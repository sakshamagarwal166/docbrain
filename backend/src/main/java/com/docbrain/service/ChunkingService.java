package com.docbrain.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ChunkingService {

    private static final Pattern PARAGRAPH_SPLIT = Pattern.compile("\\n\\s*\\n");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");
    private static final Pattern WORD_SPLIT = Pattern.compile("\\s+");

    private final int chunkSize;
    private final int chunkOverlap;

    public ChunkingService(@Value("${docbrain.chunking.chunk-size}") int chunkSize,
                           @Value("${docbrain.chunking.chunk-overlap}") int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<String> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> segments = splitRecursive(text);
        return mergeSegments(segments);
    }

    private List<String> splitRecursive(String text) {
        if (text.length() <= chunkSize) {
            return List.of(text.strip());
        }

        String[] paragraphs = PARAGRAPH_SPLIT.split(text);
        if (paragraphs.length > 1) {
            List<String> result = new ArrayList<>();
            for (String paragraph : paragraphs) {
                result.addAll(splitRecursive(paragraph));
            }
            return result;
        }

        String[] sentences = SENTENCE_SPLIT.split(text);
        if (sentences.length > 1) {
            List<String> result = new ArrayList<>();
            for (String sentence : sentences) {
                result.addAll(splitRecursive(sentence));
            }
            return result;
        }

        String[] words = WORD_SPLIT.split(text);
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() + word.length() + 1 > chunkSize && !current.isEmpty()) {
                result.add(current.toString().strip());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) {
                current.append(" ");
            }
            current.append(word);
        }
        if (!current.isEmpty()) {
            result.add(current.toString().strip());
        }
        return result;
    }

    private List<String> mergeSegments(List<String> segments) {
        if (segments.isEmpty()) {
            return segments;
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String segment : segments) {
            if (current.length() + segment.length() + 1 > chunkSize && !current.isEmpty()) {
                String chunk = current.toString().strip();
                if (!chunk.isEmpty()) {
                    chunks.add(chunk);
                }
                String overlapText = getOverlapText(chunk);
                current = new StringBuilder(overlapText);
            }
            if (!current.isEmpty() && !current.toString().isBlank()) {
                current.append(" ");
            }
            current.append(segment);
        }

        if (!current.isEmpty()) {
            String chunk = current.toString().strip();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    private String getOverlapText(String text) {
        if (chunkOverlap <= 0 || text.length() <= chunkOverlap) {
            return "";
        }
        String tail = text.substring(text.length() - chunkOverlap);
        int spaceIdx = tail.indexOf(' ');
        if (spaceIdx >= 0) {
            return tail.substring(spaceIdx + 1);
        }
        return tail;
    }
}
