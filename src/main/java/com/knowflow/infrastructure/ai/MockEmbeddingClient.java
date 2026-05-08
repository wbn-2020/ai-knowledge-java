package com.knowflow.infrastructure.ai;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Primary
@Component
public class MockEmbeddingClient implements EmbeddingClient {
    private static final int DIMENSION = 64;

    @Override
    public double[] embed(String text) {
        double[] vector = new double[DIMENSION];
        byte[] bytes = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            vector[i % DIMENSION] += (bytes[i] & 0xff) / 255.0;
        }
        byte[] digest = sha256(bytes);
        for (int i = 0; i < digest.length; i++) {
            vector[i % DIMENSION] += (digest[i] & 0xff) / 1024.0;
        }
        return vector;
    }

    private byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
