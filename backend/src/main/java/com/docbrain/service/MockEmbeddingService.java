package com.docbrain.service;

import java.util.Random;

public class MockEmbeddingService implements EmbeddingService {

    private final int dimension;

    public MockEmbeddingService(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public float[] embed(String text) {
        Random random = new Random(text.hashCode());
        float[] embedding = new float[dimension];
        float norm = 0f;
        for (int i = 0; i < dimension; i++) {
            embedding[i] = random.nextFloat() * 2 - 1;
            norm += embedding[i] * embedding[i];
        }
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < dimension; i++) {
            embedding[i] /= norm;
        }
        return embedding;
    }
}
