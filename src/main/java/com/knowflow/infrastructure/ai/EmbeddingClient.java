package com.knowflow.infrastructure.ai;

public interface EmbeddingClient {
    double[] embed(String text);
}
